package com.lyf.composescaffold.feature.browse.playback

import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.toMusicTrack
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 决定音乐与 MV 的播放交接，不直接申请底层播放器。 */
internal class FeedPlaybackCoordinator(
    factory: PlayerFactory,
    policy: FeedPlaybackPolicy,
    private val scope: CoroutineScope,
    var onCheckpoint: (PlaybackCheckpoint) -> Unit,
    teardownDelayMs: Long = 10_000L,
    progressIntervalMs: Long = 250L,
    bufferingDelayMs: Long = 300L,
    private val musicController: MusicPlayerController? = null,
) : FeedPlaybackController {
    var musicQueueProvider: () -> List<MusicTrack> = { emptyList() }

    private val mutableState = MutableStateFlow(FeedPlaybackState())
    override val state = mutableState.asStateFlow()
    private val mutableProgress = MutableStateFlow(PlaybackProgress())
    override val progress = mutableProgress.asStateFlow()
    private val mv = MvPlaybackController(
        factory, policy, scope, { onCheckpoint(it) },
        teardownDelayMs, progressIntervalMs, bufferingDelayMs,
        beforePlay = { interruptMusic(MusicPauseReason.MV) },
    )
    override val outputs = mv.outputs
    private var wanted: FeedItem? = null
    private var primarilyVisible = true
    private var hostVisible = false
    private var pendingMusicResumeMs = 0L
    private var interruption: MusicInterruption? = null
    private var musicJob: Job? = null
    private var mvStateJob: Job? = null
    private var mvProgressJob: Job? = null

    private val isMusic: Boolean get() = wanted?.media is FeedMedia.Music

    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) {
        observeMv()
        if (wanted == item && state.value.key == item.key) return
        checkpointMusic()
        wanted = item
        mutableState.value = FeedPlaybackState(key = item.key, wantsPlay = playWhenReady)
        if (isMusic) {
            mv.reset()
            startMusic(item, positionMs, playWhenReady)
        } else {
            musicJob?.cancel()
            musicJob = null
            interruptMusic(MusicPauseReason.MV)
            mv.setHostVisible(hostVisible)
            mv.setPrimarilyVisible(primarilyVisible)
            mv.select(item, positionMs, playWhenReady)
            mutableState.value = mv.state.value
        }
    }

    override fun preload(item: FeedItem?) {
        if (!isMusic) mv.preload(item)
    }

    override fun setHostVisible(visible: Boolean) {
        hostVisible = visible
        observeMv()
        if (!isMusic) {
            mv.setHostVisible(visible)
            return
        }
        if (visible) wanted?.let(::observeMusic) else {
            checkpointMusic()
            musicJob?.cancel()
            musicJob = null
        }
    }

    override fun setPrimarilyVisible(visible: Boolean) {
        primarilyVisible = visible
        if (!isMusic) {
            mv.setPrimarilyVisible(visible)
            return
        }
        if (visible) resumeInterruptedMusic(MusicPauseReason.SCROLL)
        else interruptMusic(MusicPauseReason.SCROLL)
    }

    override fun toggle() {
        if (!isMusic) return mv.toggle()
        interruption = null
        val item = wanted ?: return
        if (musicController?.state?.value?.currentTrack == null) startMusic(item, 0, true)
        else musicController?.toggle()
    }

    override fun seekTo(positionMs: Long) {
        if (isMusic) musicController?.seekTo(positionMs) else mv.seekTo(positionMs)
    }

    override fun retry() {
        if (!isMusic) return mv.retry()
        interruption = null
        wanted?.let { startMusic(it, 0, true, restart = true) }
    }

    override fun reset() {
        close()
        mv.reset()
        wanted = null
        primarilyVisible = true
        pendingMusicResumeMs = 0
        mutableState.value = FeedPlaybackState()
        mutableProgress.value = PlaybackProgress()
    }

    override fun close() {
        checkpointMusic()
        // 离开页面允许音乐后台播放；仅撤销由未完成滑动造成的暂停。
        if (isMusic) resumeInterruptedMusic(MusicPauseReason.SCROLL)
        hostVisible = false
        musicJob?.cancel()
        musicJob = null
        mvStateJob?.cancel()
        mvStateJob = null
        mvProgressJob?.cancel()
        mvProgressJob = null
        mv.close()
    }

    private fun startMusic(item: FeedItem, positionMs: Long, playWhenReady: Boolean, restart: Boolean = false) {
        val controller = musicController ?: return
        val track = item.toMusicTrack() ?: return
        val alreadySelected = controller.state.value.currentTrack?.id == item.key
        pendingMusicResumeMs = if (alreadySelected && !restart) 0 else positionMs.coerceAtLeast(0)
        if (!alreadySelected || restart) {
            interruption = null
            controller.playTrack(track, musicQueueProvider())
            if (!playWhenReady) controller.pause()
        } else if (playWhenReady) {
            resumeInterruptedMusic()
        } else {
            interruption = null
            controller.pause()
        }
        if (!primarilyVisible) interruptMusic(MusicPauseReason.SCROLL)
        observeMusic(item)
    }

    private fun interruptMusic(reason: MusicPauseReason) {
        val controller = musicController ?: return
        val snapshot = controller.state.value
        val key = snapshot.currentTrack?.id ?: return
        if (snapshot.wantsPlay) {
            controller.pause()
            interruption = MusicInterruption(key, controller.state.value.intentVersion, reason)
        } else if (interruption?.matches(snapshot) == true) {
            interruption = interruption?.copy(reason = reason)
        }
    }

    private fun resumeInterruptedMusic(reason: MusicPauseReason? = null) {
        val controller = musicController ?: return
        val pending = interruption ?: return
        if (reason != null && pending.reason != reason) return
        interruption = null
        if (pending.matches(controller.state.value)) controller.resume()
    }

    private fun observeMv() {
        if (mvStateJob?.isActive != true) mvStateJob = scope.launch {
            mv.state.collect { if (!isMusic) mutableState.value = it }
        }
        if (mvProgressJob?.isActive != true) mvProgressJob = scope.launch {
            mv.progress.collect { if (!isMusic) mutableProgress.value = it }
        }
    }

    private fun observeMusic(item: FeedItem) {
        val controller = musicController ?: return
        musicJob?.cancel()
        musicJob = scope.launch {
            controller.state.collect { snapshot ->
                if (wanted != item) return@collect
                val key = snapshot.currentTrack?.id
                if (key != item.key) pendingMusicResumeMs = 0
                if (pendingMusicResumeMs > 0 && snapshot.seekable && key == item.key) {
                    val position = pendingMusicResumeMs
                    pendingMusicResumeMs = 0
                    controller.seekTo(position)
                }
                mutableProgress.value = PlaybackProgress(
                    snapshot.positionMs, snapshot.durationMs, snapshot.seekable, snapshot.bufferedPositionMs,
                )
                mutableState.value = FeedPlaybackState(
                    key = key ?: item.key,
                    isPlaying = snapshot.isPlaying,
                    wantsPlay = snapshot.wantsPlay || interruption?.matches(snapshot) == true,
                    buffering = snapshot.isBuffering && snapshot.wantsPlay,
                    errorCode = snapshot.errorCode,
                )
            }
        }
    }

    private fun checkpointMusic() {
        if (!isMusic) return
        val snapshot = musicController?.state?.value ?: return
        onCheckpoint(PlaybackCheckpoint(
            snapshot.currentTrack?.id ?: wanted?.key,
            snapshot.positionMs,
            snapshot.wantsPlay || interruption?.matches(snapshot) == true,
        ))
    }

    private enum class MusicPauseReason { SCROLL, MV }

    /** 用户或系统后来修改意图时，旧的自动恢复记录失效。 */
    private data class MusicInterruption(val key: String, val intentVersion: Long, val reason: MusicPauseReason) {
        fun matches(snapshot: MusicPlaybackSnapshot): Boolean =
            snapshot.currentTrack?.id == key && snapshot.intentVersion == intentVersion && !snapshot.wantsPlay
    }
}
