package com.lyf.composescaffold.core.player.media3

import com.lyf.composescaffold.core.player.service.MediaSessionOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.lyf.composescaffold.core.common.log.AppLogger
import com.lyf.composescaffold.core.player.media.MediaSource
import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.media.MediaKind
import com.lyf.composescaffold.core.player.playback.PlayerState
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.Player as CorePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.net.toUri

/** 单媒体播放器的 Media3 实现。 */
@UnstableApi
internal class Media3Player(
    override val source: MediaSource,
    /** 本次播放使用的选项。 */
    private val options: PlaybackOptions,
    /** 底层 ExoPlayer，由对象池创建和回收。 */
    val player: ExoPlayer,
    /** 管理系统会话和前台服务。 */
    private val sessionManager: MediaSessionOwner,
) : CorePlayer {
    /** 主线程更新的状态，对外暴露只读流。 */
    private val mutableState = MutableStateFlow(PlayerState())
    override val state = mutableState.asStateFlow()

    /** 视频输出接口，音频为 null。 */
    override val video = if (source.kind == MediaKind.VIDEO) {
        Media3VideoOutput(player, source.id, source.initialAspectRatio)
    } else null

    /** 标记已归还的播放器，阻止后续播放命令和事件更新。 */
    private var detached = false

    /** 区分本模块设置的播放意图与外部变化。 */
    private var settingPlayIntent = false

    /** 直接读取进度，将未知的负数时间归零。 */
    override val position: PlaybackProgress
        get() = PlaybackProgress(
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            seekable = player.isCurrentMediaItemSeekable && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
        )

    /** 将播放事件和错误汇总到 state。 */
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (detached) return
            val error = player.playerError
            mutableState.value = mutableState.value.copy(
                isPlaying = player.isPlaying,
                wantsPlay = player.playWhenReady,
                ended = player.playbackState == Player.STATE_ENDED,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                errorCode = if (state.value.backgroundStartFailed) BACKGROUND_START_ERROR else error?.errorCode,
                decoderFailed = error?.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error?.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED,
                durationMs = position.durationMs,
                seekable = position.seekable,
            )
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!detached && !settingPlayIntent) {
                mutableState.value = mutableState.value.copy(
                    wantsPlay = playWhenReady,
                    externalIntentVersion = state.value.externalIntentVersion + 1,
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!detached) AppLogger.warning("MediaPlayback") {
                "媒体失败 item=${source.id.hashCode()} code=${error.errorCode}"
            }
        }
    }

    /** 记录首帧、音频输出和掉帧事件。 */
    private val analytics = object : AnalyticsListener {
        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
            reportOutput()
        }
        override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {
            if (source.kind == MediaKind.AUDIO) reportOutput()
        }
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            if (!detached && player.isPlaying) AppLogger.debug("MediaPlayback") {
                "视频掉帧=$droppedFrames 窗口ms=$elapsedMs"
            }
        }
    }

    /** 注册监听并预备当前媒体。 */
    fun prepare(positionMs: Long) {
        player.addListener(listener)
        player.addAnalyticsListener(analytics)
        player.playWhenReady = false
        player.repeatMode = if (options.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(source.title)
            .setArtist(source.artist)
            .setArtworkUri(source.artworkUri?.toUri())
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(source.id)
            .setUri(source.uri)
            .setMimeType(source.mimeType)
            .setMediaMetadata(metadata)
            .build()
        player.setMediaItem(mediaItem, positionMs)
        player.prepare()
    }

    /** 设置播放意图；后台启动失败写入 state。 */
    override fun setPlaying(playing: Boolean) {
        if (detached) return
        if (playing) {
            mutableState.value = state.value.copy(backgroundStartFailed = false, errorCode = player.playerError?.errorCode)
            val activated = sessionManager.activate(player, options.backgroundPlayback) {
                mutableState.value = state.value.copy(
                    wantsPlay = false,
                    isPlaying = false,
                    backgroundStartFailed = true,
                    errorCode = BACKGROUND_START_ERROR,
                )
            }
            if (!activated) return
        }
        settingPlayIntent = true
        try {
            player.playWhenReady = playing
        } finally {
            settingPlayIntent = false
        }
        mutableState.value = state.value.copy(wantsPlay = player.playWhenReady)
    }

    /** 普通 Seek 保留当前画面，不假定每次都会产生首帧事件。 */
    override fun seekTo(positionMs: Long) {
        val current = position
        if (detached || !current.seekable || current.durationMs <= 0) return
        val target = positionMs.coerceIn(0, current.durationMs)
        if (target == current.positionMs) return
        player.seekTo(target)
    }

    /** 临时网络问题重试：重新 prepare 当前媒体。 */
    override fun retry() {
        if (!detached) player.prepare()
    }

    /** 停止播放并移除监听，供对象池回收。 */
    fun detach() {
        sessionManager.detach(player)
        player.pause()
        detached = true
        player.removeListener(listener)
        player.removeAnalyticsListener(analytics)
        video?.close()
        player.clearVideoSurface()
    }

    private companion object {
        /** 本模块定义的后台启动失败错误码。 */
        const val BACKGROUND_START_ERROR = -2
    }

    /** 记录输出次数供诊断；视频封面只由 VideoOutput.covered 控制。 */
    private fun reportOutput() {
        if (!detached) mutableState.value = state.value.copy(outputVersion = state.value.outputVersion + 1)
    }
}
