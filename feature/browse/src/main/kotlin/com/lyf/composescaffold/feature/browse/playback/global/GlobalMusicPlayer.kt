package com.lyf.composescaffold.feature.browse.playback.global

import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.media.MediaKind
import com.lyf.composescaffold.core.player.media.MediaSource
import com.lyf.composescaffold.core.player.playback.PlayerState
import com.lyf.composescaffold.core.player.playback.PlayerPool
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import com.lyf.composescaffold.core.player.playback.Player
import com.lyf.composescaffold.core.common.log.AppLogger
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicPlaybackStatus
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode
import com.lyf.composescaffold.feature.browse.playback.di.MusicPlaybackScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 管理音乐队列与播放意图；媒体加载、缓存和系统会话交给共享播放器。 */
@Singleton
internal class GlobalMusicPlayer @Inject constructor(
    private val factory: PlayerFactory,
    @param:MusicPlaybackScope private val scope: CoroutineScope,
) : MusicPlayerController {
    private val mutableQueue = MutableStateFlow(MusicQueue())
    override val queue = mutableQueue.asStateFlow()
    private val mutableState = MutableStateFlow(MusicPlaybackSnapshot())
    override val state = mutableState.asStateFlow()
    private var pool: PlayerPool? = null
    private var player: Player? = null
    private var prepareJob: Job? = null
    private var observeJob: Job? = null
    private var progressJob: Job? = null
    private var generation = 0L

    override fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>?) {
        val tracks = (newQueue ?: queue.value.tracks).toMutableList()
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index < 0) tracks.add(track) else tracks[index] = track
        mutableQueue.value = MusicQueue(tracks.distinctBy { it.id }, playMode = queue.value.playMode)
            .let { it.copy(currentIndex = it.tracks.indexOfFirst { candidate -> candidate.id == track.id }) }
        startCurrentTrack()
    }

    override fun playAt(index: Int) {
        if (index !in queue.value.tracks.indices) return
        mutableQueue.update { it.copy(currentIndex = index) }
        startCurrentTrack()
    }

    override fun pause() {
        // 加载尚未完成时也要撤销播放意图。
        mutableState.update { it.copy(wantsPlay = false, intentVersion = it.intentVersion + 1) }
        player?.setPlaying(false)
    }

    override fun resume() {
        val current = player
        if (current == null) {
            if (prepareJob?.isActive == true) mutableState.update { it.copy(wantsPlay = true, intentVersion = it.intentVersion + 1) }
            else if (!queue.value.isEmpty) startCurrentTrack()
            return
        }
        mutableState.update { it.copy(wantsPlay = true, errorCode = null, intentVersion = it.intentVersion + 1) }
        if (current.state.value.ended) current.seekTo(0)
        if (current.state.value.errorCode != null) current.retry()
        current.setPlaying(true)
    }

    override fun toggle() {
        if (state.value.wantsPlay) pause() else resume()
    }

    override fun seekTo(positionMs: Long) {
        val current = player ?: return
        if (!current.position.seekable) return
        current.seekTo(positionMs)
        publishProgress(current)
    }

    override fun next() { queue.value.nextIndex()?.let(::playAt) }
    override fun previous() { queue.value.previousIndex()?.let(::playAt) }
    override fun setPlayMode(mode: PlayMode) { mutableQueue.update { it.copy(playMode = mode) } }

    override fun removeFromQueue(trackId: String) {
        val removedCurrent = queue.value.currentTrack?.id == trackId
        val remaining = queue.value.remove(trackId) ?: return clearQueue()
        mutableQueue.value = remaining
        if (removedCurrent) startCurrentTrack()
    }

    override fun clearQueue() {
        generation++
        cancelWork()
        releasePlayer()
        pool?.release()
        pool = null
        mutableQueue.value = MusicQueue()
        mutableState.value = MusicPlaybackSnapshot(intentVersion = state.value.intentVersion + 1)
    }

    private fun startCurrentTrack() {
        val track = queue.value.currentTrack ?: return
        val ticket = ++generation
        cancelWork()
        releasePlayer()
        // 新请求不继承旧曲目的错误、进度或缓冲状态。
        mutableState.value = MusicPlaybackSnapshot(
            intentVersion = state.value.intentVersion + 1,
            currentTrack = track,
            status = MusicPlaybackStatus.BUFFERING,
            wantsPlay = true,
        )
        prepareJob = scope.launch {
            try {
                val currentPool = pool ?: factory.createPool(1).also { created ->
                    if (ticket != generation) {
                        created.release()
                        return@launch
                    }
                    pool = created
                }
                val acquired = currentPool.acquire(track.toMediaSource(), 0, PlaybackOptions(backgroundPlayback = true))
                if (ticket != generation) {
                    currentPool.recycle(acquired)
                    return@launch
                }
                player = acquired
                acquired.setPlaying(state.value.wantsPlay)
                observeJob = scope.launch {
                    var externalVersion = acquired.state.value.externalIntentVersion
                    acquired.state.collect { status ->
                        if (player === acquired && externalVersion != status.externalIntentVersion) {
                            mutableState.update { it.copy(intentVersion = it.intentVersion + 1) }
                            externalVersion = status.externalIntentVersion
                        }
                        if (player === acquired) publishState(acquired, status)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (ticket == generation) {
                    releasePlayer()
                    mutableState.update { it.copy(status = MusicPlaybackStatus.ERROR, errorCode = -1, wantsPlay = false) }
                    AppLogger.warning("GlobalMusicPlayer") { "音乐初始化失败 type=${error.javaClass.simpleName}" }
                }
            }
        }
    }

    private fun publishState(current: Player, status: PlayerState) {
        val position = current.position
        mutableState.update {
            it.copy(
                status = when {
                    status.errorCode != null -> MusicPlaybackStatus.ERROR
                    status.isBuffering -> MusicPlaybackStatus.BUFFERING
                    status.isPlaying -> MusicPlaybackStatus.PLAYING
                    else -> MusicPlaybackStatus.PAUSED
                },
                wantsPlay = status.wantsPlay,
                errorCode = status.errorCode,
                positionMs = position.positionMs,
                durationMs = position.durationMs,
                bufferedPositionMs = position.bufferedPositionMs,
                seekable = position.seekable,
            )
        }
        if (status.ended && status.wantsPlay) {
            if (queue.value.playMode == PlayMode.SINGLE_LOOP) {
                current.seekTo(0)
                current.setPlaying(true)
            } else {
                next()
            }
            return
        }
        if (!status.isPlaying) {
            progressJob?.cancel()
            progressJob = null
        } else if (progressJob?.isActive != true) {
            progressJob = scope.launch {
                while (isActive && player === current) {
                    delay(250)
                    publishProgress(current)
                }
            }
        }
    }

    private fun publishProgress(current: Player) {
        val value = current.position
        mutableState.update {
            it.copy(positionMs = value.positionMs, durationMs = value.durationMs,
                bufferedPositionMs = value.bufferedPositionMs, seekable = value.seekable)
        }
    }

    private fun releasePlayer() {
        player?.let { current ->
            current.setPlaying(false)
            pool?.recycle(current)
        }
        player = null
    }

    private fun cancelWork() {
        prepareJob?.cancel()
        observeJob?.cancel()
        progressJob?.cancel()
        prepareJob = null
        observeJob = null
        progressJob = null
    }
}

private fun MusicTrack.toMediaSource() = MediaSource(
    id = id, uri = url, kind = MediaKind.AUDIO, title = title, artist = artist, artworkUri = coverUrl,
)
