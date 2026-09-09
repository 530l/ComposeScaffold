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
    private val factory: PlayerFactory, // 创建通用播放器池，业务不依赖 Media3 类型。
    @param:MusicPlaybackScope private val scope: CoroutineScope, // 音乐专用协程作用域，页面退出不取消任务。
) : MusicPlayerController {
    private val mutableQueue = MutableStateFlow(MusicQueue())
    // 外部只读队列，通过命令修改它。
    override val queue = mutableQueue.asStateFlow()
    private val mutableState = MutableStateFlow(MusicPlaybackSnapshot())
    override val state = mutableState.asStateFlow()
    // 按需创建播放器池，释放后清空引用。
    private var pool: PlayerPool? = null
    // 音乐路径只持有一个当前播放器。
    private var player: Player? = null
    private var prepareJob: Job? = null
    private var observeJob: Job? = null
    private var progressJob: Job? = null
    // 用请求代次识别已经过时的异步结果：迟到的 acquire 一律丢弃。
    private var generation = 0L

    override fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>?) {
        val tracks = (newQueue ?: queue.value.tracks).toMutableList()
        val index = tracks.indexOfFirst { it.id == track.id }
        // 已在队列时用传入曲目覆盖原项，以刷新最新媒体信息。
        if (index < 0) tracks.add(track) else tracks[index] = track
        // 去重并保留用户选择的循环模式。
        mutableQueue.value = MusicQueue(tracks.distinctBy { it.id }, playMode = queue.value.playMode)
            // 去重可能移动元素，需在最终列表上重算当前索引。
            .let { it.copy(currentIndex = it.tracks.indexOfFirst { candidate -> candidate.id == track.id }) }
        startCurrentTrack()
    }

    override fun playAt(index: Int) {
        if (index !in queue.value.tracks.indices) return
        mutableQueue.update { it.copy(currentIndex = index) }
        startCurrentTrack()
    }

    // 先记意图再操作播放器：播放器尚未创建或仍在加载时，暂停也必须生效。
    override fun pause() {
        // intentVersion 乐观锁：递增版本令过期的自动恢复请求失效。
        mutableState.update { it.copy(wantsPlay = false, intentVersion = it.intentVersion + 1) }
        player?.setPlaying(false)
    }

    // 恢复当前曲目，必要时重新准备或重试。
    override fun resume() {
        // 固定引用快照，避免判空与使用之间被切歌置空。
        val current = player
        if (current == null) {
            // 准备仍在进行时只更新意图，准备完成后会按最新意图起播。
            if (prepareJob?.isActive == true) mutableState.update { it.copy(wantsPlay = true, intentVersion = it.intentVersion + 1) }
            // 队列有曲目但连准备任务都没有（如进程重启后）才重新加载。
            else if (!queue.value.isEmpty) startCurrentTrack()
            return
        }
        mutableState.update { it.copy(wantsPlay = true, errorCode = null, intentVersion = it.intentVersion + 1) }
        // 曲目已自然播完，从头开始。
        if (current.state.value.ended) current.seekTo(0)
        // 出错过的曲目先让底层重试再起播。
        if (current.state.value.errorCode != null) current.retry()
        current.setPlaying(true)
    }

    // 按 wantsPlay 而非 isPlaying 判断：缓冲中（想播但未出声）也能正确暂停。
    override fun toggle() {
        if (state.value.wantsPlay) pause() else resume()
    }

    override fun seekTo(positionMs: Long) {
        val current = player ?: return
        // 不可定位的媒体（如直播流）不接受拖动。
        if (!current.position.seekable) return
        current.seekTo(positionMs)
        // 立即刷新位置，不等下一轮 250ms 采样，进度条不回跳。
        publishProgress(current)
    }

    // 由队列循环模式决定下一首，无下一首则不切换。
    override fun next() { queue.value.nextIndex()?.let(::playAt) }
    // 由队列规则决定上一首。
    override fun previous() { queue.value.previousIndex()?.let(::playAt) }
    // 只更新循环模式，不重建播放器。
    override fun setPlayMode(mode: PlayMode) { mutableQueue.update { it.copy(playMode = mode) } }

    override fun removeFromQueue(trackId: String) {
        val removedCurrent = queue.value.currentTrack?.id == trackId
        // 删空队列时连带释放全部音乐资源。
        val remaining = queue.value.remove(trackId) ?: return clearQueue()
        mutableQueue.value = remaining
        if (removedCurrent) startCurrentTrack()
    }

    override fun clearQueue() {
        // 递增代次，使尚未完成的旧准备结果失效。
        generation++
        cancelWork()
        releasePlayer()
        pool?.release()
        // 清空旧池引用，下次播放时重新创建。
        pool = null
        mutableQueue.value = MusicQueue()
        // 快照清零并递增意图版本，令旧的自动恢复记录失效。
        mutableState.value = MusicPlaybackSnapshot(intentVersion = state.value.intentVersion + 1)
    }

    // 加载当前曲目；用代次保证只有最新一次请求的结果会被采用。
    private fun startCurrentTrack() {
        val track = queue.value.currentTrack ?: return
        // 本次请求的编号，之前的异步结果凭它识别并丢弃。
        val ticket = ++generation
        cancelWork()
        releasePlayer()
        // 新请求不继承旧曲目的错误、进度或缓冲状态。
        mutableState.value = MusicPlaybackSnapshot(
            // 切歌属于新的播放意图。
            intentVersion = state.value.intentVersion + 1,
            // 先把目标曲目通知 UI。
            currentTrack = track,
            status = MusicPlaybackStatus.BUFFERING,
            wantsPlay = true,
        )
        prepareJob = scope.launch {
            try {
                val acquired = preparePlayer(track, ticket) ?: return@launch
                player = acquired
                // 准备耗时，期间用户可能已暂停：按最新意图而非默认值起播。
                acquired.setPlaying(state.value.wantsPlay)
                observePlayer(acquired)
            // 取消必须重新抛出，不能当作播放错误吞掉。
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handlePrepareFailure(ticket, error)
            }
        }
    }

    private fun publishState(current: Player, status: PlayerState) {
        val position = current.position
        mutableState.update {
            it.copy(
                // 展示状态按错误＞缓冲＞播放的优先级归并。
                status = when {
                    status.errorCode != null -> MusicPlaybackStatus.ERROR
                    status.isBuffering -> MusicPlaybackStatus.BUFFERING
                    status.isPlaying -> MusicPlaybackStatus.PLAYING
                    else -> MusicPlaybackStatus.PAUSED
                },
                wantsPlay = status.wantsPlay,
                // 透传脱敏错误码供界面展示。
                errorCode = status.errorCode,
                positionMs = position.positionMs,
                durationMs = position.durationMs,
                bufferedPositionMs = position.bufferedPositionMs,
                seekable = position.seekable,
            )
        }
        // 自然播完且仍想播时按队列规则衔接。
        if (status.ended && status.wantsPlay) {
            // 单曲循环在本地回到开头，其余模式交给队列的循环规则选下一首。
            if (queue.value.playMode == PlayMode.SINGLE_LOOP) {
                current.seekTo(0)
                current.setPlaying(true)
            } else {
                next()
            }
            return
        }
        // 暂停、缓冲或错误时停掉定时采样。
        if (!status.isPlaying) {
            progressJob?.cancel()
            progressJob = null
        } else if (progressJob?.isActive != true) {
            progressJob = scope.launch {
                // 仍是当前播放器才继续采样，切歌后旧采样自然退出。
                while (isActive && player === current) {
                    // 250ms 采样：进度条够顺滑，又不至于高频刷新状态流。
                    delay(250)
                    publishProgress(current)
                }
            }
        }
    }

    // 只更新高频位置字段，不触碰播放意图，避免覆盖用户操作。
    private fun publishProgress(current: Player) {
        val value = current.position
        mutableState.update {
            it.copy(positionMs = value.positionMs, durationMs = value.durationMs,
                bufferedPositionMs = value.bufferedPositionMs, seekable = value.seekable)
        }
    }

    // 归还当前音乐播放器；只回收租约不释放池，留待下一首复用。
    private fun releasePlayer() {
        player?.let { current ->
            // 归还前先停声，池里不带播放中的播放器。
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

    // 准备并获取当前曲目播放器；任一步发现代次过时即归还资源并返回 null，由调用方静默放弃。
    private suspend fun preparePlayer(track: MusicTrack, ticket: Long): Player? {
        // 音乐路径独占容量为一的池，池跨曲目复用。
        val currentPool = pool ?: factory.createPool(1).also { created ->
            // 池创建期间用户又切歌，本次创建已过时。
            if (ticket != generation) {
                created.release()
                return null
            }
            pool = created
        }
        // 从头准备音频，并允许底层建立后台播放会话。
        val acquired = currentPool.acquire(track.toMediaSource(), 0, PlaybackOptions(backgroundPlayback = true))
        // acquire 期间用户又切歌，结果同样过时。
        if (ticket != generation) {
            // 过时申请必须归还原池，否则泄漏租约。
            currentPool.recycle(acquired)
            return null
        }
        return acquired
    }

    // 观察已采用的播放器：外部操作递增业务意图版本，状态统一经 publishState 发布。
    private fun observePlayer(acquired: Player) {
        observeJob = scope.launch {
            // 记录已处理的外部操作版本（通知栏、耳机键等底层操作）。
            var externalVersion = acquired.state.value.externalIntentVersion
            acquired.state.collect { status ->
                // 只有当前播放器出现新的外部操作才递增业务意图版本。
                if (player === acquired && externalVersion != status.externalIntentVersion) {
                    // 外部操作改变现状，令旧的自动恢复请求失效。
                    mutableState.update { it.copy(intentVersion = it.intentVersion + 1) }
                    externalVersion = status.externalIntentVersion
                }
                // 旧播放器的迟到回调不能覆盖新曲目状态。
                if (player === acquired) publishState(acquired, status)
            }
        }
    }

    // 过时任务失败不污染当前曲目。
    private fun handlePrepareFailure(ticket: Long, error: Exception) {
        if (ticket == generation) {
            releasePlayer()
            mutableState.update { it.copy(status = MusicPlaybackStatus.ERROR, errorCode = -1, wantsPlay = false) }
            // 只记录异常类型，不输出地址或异常正文。
            AppLogger.warning("GlobalMusicPlayer") { "音乐初始化失败 type=${error.javaClass.simpleName}" }
        }
    }
}

// 在音乐边界把业务曲目转为通用音频源。
private fun MusicTrack.toMediaSource() = MediaSource(
    // 传递音频地址和展示元数据，不携带 Feed 业务状态。
    id = id, uri = url, kind = MediaKind.AUDIO, title = title, artist = artist, artworkUri = coverUrl,
)
