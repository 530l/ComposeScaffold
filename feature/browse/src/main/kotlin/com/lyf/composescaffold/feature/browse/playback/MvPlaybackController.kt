package com.lyf.composescaffold.feature.browse.playback

import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
import com.lyf.composescaffold.core.player.playback.PlayerPool
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import com.lyf.composescaffold.core.player.playback.Player
import com.lyf.composescaffold.core.common.log.AppLogger
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 只负责 MV 播放器池、预备、进度和资源释放。 */
internal class MvPlaybackController(
    private val factory: PlayerFactory,
    private val policy: FeedPlaybackPolicy,
    private val scope: CoroutineScope,
    var onCheckpoint: (PlaybackCheckpoint) -> Unit,
    private val teardownDelayMs: Long = TEARDOWN_DELAY_MS,
    private val progressIntervalMs: Long = PROGRESS_INTERVAL_MS,
    private val bufferingDelayMs: Long = BUFFERING_DELAY_MS,
    private val beforePlay: () -> Unit = {},
) : FeedPlaybackController {
    private val mutableState = MutableStateFlow(FeedPlaybackState())
    override val state = mutableState.asStateFlow()
    private val mutableProgress = MutableStateFlow(PlaybackProgress())
    override val progress = mutableProgress.asStateFlow()
    private val mutableOutputs = MutableStateFlow<Map<String, VideoOutput>>(emptyMap())
    override val outputs = mutableOutputs.asStateFlow()

    private val players = mutableMapOf<String, Player>()
    private val observers = mutableMapOf<String, Job>()
    private var pool: PlayerPool? = null
    private var reconcileJob: Job? = null
    private var progressJob: Job? = null
    private var bufferingJob: Job? = null
    private var teardownJob: Job? = null

    private var generation = 0L
    private var capacity = policy.capacity(factory.lowRam)
    private var hostVisible = false
    private var primarilyVisible = true
    private var wanted: FeedItem? = null
    private var neighbor: FeedItem? = null
    private var resumePosition = 0L
    private var wantsPlay = true
    private var activatedAt = 0L
    private var outputReported = false

    override fun setHostVisible(visible: Boolean) {
        if (hostVisible == visible) return
        hostVisible = visible
        AppLogger.debug(TAG) { "页面可见=$visible" }
        if (visible) {
            teardownJob?.cancel()
            teardownJob = null
            if (pool != null && active() != null) {
                applyPlayback()
            } else {
                schedule()
            }
        } else {
            checkpoint()
            stopTimers()
            active()?.setPlaying(false)
            mutableState.value = state.value.copy(isPlaying = false, buffering = false)
            teardownJob?.cancel()
            teardownJob = scope.launch {
                delay(teardownDelayMs)
                AppLogger.debug(TAG) { "后台停留超时，释放视频播放器池" }
                releasePlayers()
            }
        }
    }

    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) {
        require(item.media is FeedMedia.Mv)
        if (wanted == item && state.value.key == item.key) return
        checkpoint()
        players.values.forEach { it.setPlaying(false) }
        stopTimers()
        wanted = item
        wantsPlay = playWhenReady
        activatedAt = System.nanoTime()
        outputReported = false
        resumePosition = positionMs.coerceAtLeast(0)
        mutableState.value = FeedPlaybackState(key = item.key, wantsPlay = playWhenReady)
        players[item.key]?.seekTo(resumePosition)
        schedule()
    }

    override fun preload(item: FeedItem?) {
        // 音乐由全局队列播放，不创建无法接管的音频预备播放器。
        val candidate = item?.takeIf { it.media is FeedMedia.Mv }
        if (neighbor == candidate) return
        neighbor = candidate
        if (capacity > 1 && hostVisible) schedule()
    }

    override fun setPrimarilyVisible(visible: Boolean) {
        if (primarilyVisible == visible) return
        primarilyVisible = visible
        applyPlayback()
    }

    override fun toggle() {
        wantsPlay = !wantsPlay
        applyPlayback()
    }

    override fun seekTo(positionMs: Long) {
        active()?.let {
            it.seekTo(positionMs)
            publishProgress()
        } ?: run {
            resumePosition = positionMs
        }
    }

    override fun retry() {
        active()?.let {
            it.retry()
            applyPlayback()
        } ?: schedule()
    }

    override fun reset() {
        releasePlayers()
        wanted = null
        neighbor = null
        resumePosition = 0
        wantsPlay = true
        mutableState.value = FeedPlaybackState()
        mutableProgress.value = PlaybackProgress()
    }

    override fun close() {
        hostVisible = false
        releasePlayers()
    }

    private fun active(): Player? = wanted?.key?.let(players::get)

    private fun schedule() {
        reconcileJob?.cancel()
        val ticket = ++generation
        val target = wanted
        if (!hostVisible || target == null || target.media is FeedMedia.Music) return
        reconcileJob = scope.launch {
            try {
                val currentPool = pool ?: factory.createPool(capacity).also { created ->
                    if (ticket != generation || !hostVisible) {
                        created.release()
                        return@launch
                    }
                    pool = created
                }
                val current = wanted ?: return@launch
                val candidate = neighbor?.takeIf { capacity > 1 && it.key != current.key }
                val allowed = setOfNotNull(current.key, candidate?.key)
                players.values.toList().filter {
                    it.source.id !in allowed || (it.source.id == current.key && it.source != current.toMediaSource())
                }.forEach(::recycle)

                acquire(currentPool, current, ticket, resumePosition)
                if (ticket != generation) return@launch
                applyPlayback()

                if (candidate != null) acquire(currentPool, candidate, ticket, 0)
                AppLogger.debug(TAG) { "播放器租约数=${players.size} 容量=$capacity" }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (ticket == generation) {
                    if (active() == null) mutableState.value = state.value.copy(errorCode = INITIALIZATION_ERROR)
                    AppLogger.warning(TAG) { "播放器初始化失败 type=${error.javaClass.simpleName}" }
                }
            }
        }
    }

    private suspend fun acquire(currentPool: PlayerPool, item: FeedItem, ticket: Long, position: Long) {
        if (item.key in players) return
        val player = currentPool.acquire(item.toMediaSource(), position, PlaybackOptions(repeatOne = true))
        if (ticket != generation || !hostVisible) {
            currentPool.recycle(player)
            return
        }
        players[item.key] = player
        player.video?.let { mutableOutputs.value += item.key to it }
        observers[item.key] = scope.launch {
            var externalIntentVersion = player.state.value.externalIntentVersion
            player.state.collect { status ->
                if (players[item.key] !== player) return@collect
                if (status.decoderFailed && capacity > 1) {
                    capacity = 1
                    releasePlayers()
                    schedule()
                    return@collect
                }
                if (active() === player) {
                    if (status.externalIntentVersion != externalIntentVersion) {
                        wantsPlay = status.wantsPlay && hostVisible
                        if (!wantsPlay) player.setPlaying(false)
                        checkpoint()
                    }
                    externalIntentVersion = status.externalIntentVersion
                    publishState()
                }
            }
        }
    }

    private fun applyPlayback() {
        val current = active()
        players.values.filter { it !== current }.forEach { it.setPlaying(false) }
        val shouldPlay = policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
        if (shouldPlay && current != null) beforePlay()
        current?.setPlaying(shouldPlay)
        mutableState.value = state.value.copy(wantsPlay = wantsPlay)
        publishState()
    }

    private fun publishState() {
        val status = active()?.state?.value ?: return
        mutableState.value = state.value.copy(
            isPlaying = status.isPlaying,
            wantsPlay = wantsPlay,
            errorCode = status.errorCode,
        )
        val allowed = policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
        updateBuffering(allowed && status.isBuffering && status.errorCode == null)
        if (allowed && status.outputVersion > 0 && !outputReported) {
            outputReported = true
            AppLogger.debug(TAG) { "切换输出耗时ms=${(System.nanoTime() - activatedAt) / 1_000_000}" }
        }
        publishProgress()
        if (status.isPlaying && hostVisible && progressJob?.isActive != true) {
            progressJob = scope.launch {
                var ticks = 0
                while (active()?.state?.value?.isPlaying == true) {
                    delay(progressIntervalMs)
                    publishProgress()
                    if (++ticks % 15 == 0) checkpoint()
                }
            }
        }
    }

    private fun updateBuffering(isBuffering: Boolean) {
        if (!isBuffering) {
            bufferingJob?.cancel()
            bufferingJob = null
            mutableState.value = state.value.copy(buffering = false)
            return
        }
        if (state.value.buffering || bufferingJob?.isActive == true) return
        val current = active()
        bufferingJob = scope.launch {
            delay(bufferingDelayMs)
            if (active() !== current || current?.state?.value?.isBuffering != true ||
                !policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
            ) return@launch
            mutableState.value = state.value.copy(buffering = true)
        }
    }

    private fun publishProgress() {
        active()?.let {
            val p = it.position
            mutableProgress.value = p
            resumePosition = p.positionMs
        }
    }

    private fun checkpoint() {
        if (wanted == null) return
        resumePosition = active()?.position?.positionMs ?: resumePosition
        onCheckpoint(PlaybackCheckpoint(wanted?.key, resumePosition, wantsPlay))
    }

    private fun recycle(player: Player) {
        players.remove(player.source.id)
        observers.remove(player.source.id)?.cancel()
        mutableOutputs.value -= player.source.id
        pool?.recycle(player)
    }

    private fun stopTimers() {
        progressJob?.cancel()
        progressJob = null
        bufferingJob?.cancel()
        bufferingJob = null
    }

    /** 归还租约时，播放器自行解绑它拥有的系统会话。 */
    private fun releaseLeases() {
        generation++
        reconcileJob?.cancel()
        reconcileJob = null
        stopTimers()
        players.values.toList().forEach(::recycle)
        pool?.release()
        pool = null
    }

    private fun releasePlayers() {
        checkpoint()
        teardownJob?.cancel()
        teardownJob = null
        releaseLeases()
        mutableState.value = state.value.copy(isPlaying = false, buffering = false)
    }

    companion object {
        private const val TAG = "FeedVideoPlayback"
        private const val INITIALIZATION_ERROR = -1
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val BUFFERING_DELAY_MS = 300L
        private const val TEARDOWN_DELAY_MS = 10_000L
    }
}
