package com.lyf.composescaffold.feature.browse.presentation.mv.playback

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
    private val factory: PlayerFactory, // 创建通用播放器池，业务不依赖 Media3 类型。
    private val policy: FeedPlaybackPolicy,
    private val scope: CoroutineScope,
    var onCheckpoint: (PlaybackCheckpoint) -> Unit, // 把作品位置和播放意图交回上层保存。
    private val teardownDelayMs: Long = TEARDOWN_DELAY_MS, // 页面不可见多久后释放视频池。
    private val progressIntervalMs: Long = PROGRESS_INTERVAL_MS,
    private val bufferingDelayMs: Long = BUFFERING_DELAY_MS, // 缓冲持续超过该时长才显示提示。
    private val beforePlay: () -> Unit = {}, // 视频起播前通知外层协调其他声音。
) : FeedPlaybackController {
    private val mutableState = MutableStateFlow(FeedPlaybackState())
    override val state = mutableState.asStateFlow()
    // 单独保存进度，避免带动列表刷新。
    private val mutableProgress = MutableStateFlow(PlaybackProgress())
    override val progress = mutableProgress.asStateFlow()
    // 保存已经借到的视频输出，按作品标识索引。
    private val mutableOutputs = MutableStateFlow<Map<String, VideoOutput>>(emptyMap())
    // UI 只能读取输出，不负责申请播放器。
    override val outputs = mutableOutputs.asStateFlow()

    // 只有当前作品和预备作品能持有播放器租约。
    private val players = mutableMapOf<String, Player>()
    private val observers = mutableMapOf<String, Job>()
    // 按需创建播放器池，释放后清空引用。
    private var pool: PlayerPool? = null
    private var reconcileJob: Job? = null
    private var progressJob: Job? = null
    private var bufferingJob: Job? = null
    private var teardownJob: Job? = null

    // 用请求代次识别已经过时的异步结果。
    private var generation = 0L
    // 低内存设备一开始就使用单播放器。
    private var capacity = policy.capacity(factory.lowRam)
    // 页面主动通知可见后才允许播放。
    private var hostVisible = false
    private var primarilyVisible = true
    private var wanted: FeedItem? = null
    // 滑动方向上的预备视频。
    private var neighbor: FeedItem? = null
    // 播放器尚未准备好时暂存恢复位置。
    private var resumePosition = 0L
    // 保留用户播放意图，不被临时离屏暂停覆盖。
    private var wantsPlay = true
    // 记录选中作品的时间，用于输出耗时诊断。
    private var activatedAt = 0L
    private var outputReported = false

    // 同步页面前台可见性，离屏立即停播。
    override fun setHostVisible(visible: Boolean) {
        // 可见性未变时避免重复启动释放或准备任务。
        if (hostVisible == visible) return
        hostVisible = visible
        // 日志仅记录可见性，不记录媒体地址。
        AppLogger.debug(TAG) { "页面可见=$visible" }
        if (visible) {
            teardownJob?.cancel()
            teardownJob = null
            // 当前播放器仍在池中时可以直接复用。
            if (pool != null && active() != null) {
                applyPlayback()
            } else {
                schedule()
            }
        } else {
            checkpoint()
            stopTimers()
            // 立即暂停当前播放器，避免离屏后继续出声。
            active()?.setPlaying(false)
            mutableState.value = state.value.copy(isPlaying = false, buffering = false)
            teardownJob?.cancel()
            teardownJob = scope.launch {
                // 短暂离屏先保留池，超过宽限期再释放。
                delay(teardownDelayMs)
                AppLogger.debug(TAG) { "后台停留超时，释放视频播放器池" }
                releasePlayers()
            }
        }
    }

    // 正式选中一个视频，并应用恢复位置和意图。
    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) {
        // 拒绝音乐项，音频由全局音乐控制器管理。
        require(item.media is FeedMedia.Mv)
        // 重复选中同一作品不重新起播。
        if (wanted == item && state.value.key == item.key) return
        checkpoint()
        // 切换前先暂停所有已借用播放器，避免新旧视频同时出声。
        players.values.forEach { it.setPlaying(false) }
        stopTimers()
        wanted = item
        wantsPlay = playWhenReady
        // 用单调时钟记录本次切换起点。
        activatedAt = System.nanoTime()
        outputReported = false
        resumePosition = positionMs.coerceAtLeast(0)
        mutableState.value = FeedPlaybackState(key = item.key, wantsPlay = playWhenReady)
        // 预备命中时把已有播放器定位到指定位置。
        players[item.key]?.seekTo(resumePosition)
        schedule()
    }

    // 更新唯一的相邻预备目标。
    override fun preload(item: FeedItem?) {
        // 音乐由全局队列播放，不创建无法接管的音频预备播放器。
        val candidate = item?.takeIf { it.media is FeedMedia.Mv }
        // 目标未变时不重复取消和申请。
        if (neighbor == candidate) return
        neighbor = candidate
        // 只有双播放器且页面可见时才安排预备。
        if (capacity > 1 && hostVisible) schedule()
    }

    // 滑动时通知当前视频是否仍占主要区域。
    override fun setPrimarilyVisible(visible: Boolean) {
        // 可见性未变时不重复下发播放命令。
        if (primarilyVisible == visible) return
        primarilyVisible = visible
        applyPlayback()
    }

    override fun toggle() {
        // 反转的是用户意图；缓冲中也可先记录暂停。
        wantsPlay = !wantsPlay
        applyPlayback()
    }

    override fun seekTo(positionMs: Long) {
        active()?.let {
            it.seekTo(positionMs)
            // 立即刷新位置，不等待下次定时采样。
            publishProgress()
        // 播放器尚未取得时先保存位置，申请成功后再应用。
        } ?: run {
            resumePosition = positionMs
        }
    }

    // 重试当前作品，尚无播放器时重新申请。
    override fun retry() {
        active()?.let {
            it.retry()
            applyPlayback()
        } ?: schedule()
    }

    // 清空当前视频选择，供模式切换或刷新使用。
    override fun reset() {
        releasePlayers()
        wanted = null
        neighbor = null
        resumePosition = 0
        wantsPlay = true
        mutableState.value = FeedPlaybackState()
        mutableProgress.value = PlaybackProgress()
    }

    // 关闭页面播放资源，但保留可重新选择的控制器。
    override fun close() {
        // 置为不可见，阻止尚未完成的申请在关闭后起播。
        hostVisible = false
        releasePlayers()
    }

    private fun active(): Player? = wanted?.key?.let(players::get)

    // 让池中的播放器与最新当前项、预备项保持一致。
    private fun schedule() {
        // 取消过时分配任务，让最终目标优先。
        reconcileJob?.cancel()
        // 创建本次请求的编号，使之前的结果失效。
        val ticket = ++generation
        val target = wanted
        // 离屏、无目标或音乐项都不申请视频播放器。
        if (!hostVisible || target == null || target.media is FeedMedia.Music) return
        reconcileJob = scope.launch {
            try {
                val currentPool = reuseOrCreatePool(ticket) ?: return@launch
                // 挂起后重读最新目标，避免用旧引用分配。
                val current = wanted ?: return@launch
                // 单播放器或重复目标时不分配预备项。
                val candidate = neighbor?.takeIf { capacity > 1 && it.key != current.key }
                // 只允许当前作品和一个预备作品保留租约。
                val allowed = setOfNotNull(current.key, candidate?.key)
                // 先复制集合再筛选，避免边遍历边回收。
                players.values.toList().filter {
                    // 离开预备范围，或同 key 但媒体源已变化（刷新后同 id 新地址）时归还。
                    it.source.id !in allowed || (it.source.id == current.key && it.source != current.toMediaSource())
                }.forEach(::recycle)

                // 先申请当前视频，优先满足正式播放。
                acquire(currentPool, current, ticket, resumePosition)
                // 申请是挂起点，结束后再次排除过时任务。
                if (ticket != generation) return@launch
                applyPlayback()

                // 当前项租约到手后，再申请从头播放的相邻预备视频。
                if (candidate != null) acquire(currentPool, candidate, ticket, 0)
                AppLogger.debug(TAG) { "播放器租约数=${players.size} 容量=$capacity" }
            // 取消必须原样抛出，不能当作播放错误吞掉。
            } catch (error: CancellationException) {
                throw error
            // 普通失败转为可展示的错误状态。
            } catch (error: Exception) {
                // 只把最新一代任务的失败反馈给页面。
                if (ticket == generation) {
                    // 当前播放器都未取得时显示初始化错误。
                    if (active() == null) mutableState.value = state.value.copy(errorCode = INITIALIZATION_ERROR)
                    // 仅记录异常类型，避免日志泄露媒体地址。
                    AppLogger.warning(TAG) { "播放器初始化失败 type=${error.javaClass.simpleName}" }
                }
            }
        }
    }

    // 为一个作品借用播放器，并绑定可取消的状态观察。
    private suspend fun acquire(currentPool: PlayerPool, item: FeedItem, ticket: Long, position: Long) {
        // 已持有该作品时不重复申请。
        if (item.key in players) return
        // 视频默认单条循环，循环语义由通用媒体源携带。
        val player = currentPool.acquire(item.toMediaSource(), position, PlaybackOptions(repeatOne = true))
        // 结果已过时或页面离屏时不能继续使用。
        if (ticket != generation || !hostVisible) {
            // 把迟到结果归还原池，不让它进入页面。
            currentPool.recycle(player)
            return
        }
        players[item.key] = player
        player.video?.let { mutableOutputs.value += item.key to it }
        observers[item.key] = scope.launch {
            // 外部意图乐观锁基线：版本前进说明耳机/焦点等系统入口改过播放意图。
            var externalIntentVersion = player.state.value.externalIntentVersion
            player.state.collect { status ->
                // 租约已替换时忽略旧播放器回调。
                if (players[item.key] !== player) return@collect
                // 解码资源不足时降为单播放器，本会话内不再恢复。
                if (status.decoderFailed && capacity > 1) {
                    capacity = 1
                    releasePlayers()
                    schedule()
                    // 降级已触发重建，本次旧状态不再处理。
                    return@collect
                }
                // 只有当前作品的播放器能更新页面状态，预备播放器只保持暂停。
                if (active() === player) {
                    if (status.externalIntentVersion != externalIntentVersion) {
                        // 接受外部意图，但页面已离屏时不能顺带起播。
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

    // 集中下发播放权，确保预备播放器保持暂停。
    private fun applyPlayback() {
        val current = active()
        // 先暂停其他播放器，避免同时发声。
        players.values.filter { it !== current }.forEach { it.setPlaying(false) }
        // 页面可见、作品占主区域、用户想播，三者同时成立才允许播放。
        val shouldPlay = policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
        // 真正起播前通知外层暂停其他媒体。
        if (shouldPlay && current != null) beforePlay()
        current?.setPlaying(shouldPlay)
        // 页面状态保留用户意图，不用临时离屏暂停覆盖它。
        mutableState.value = state.value.copy(wantsPlay = wantsPlay)
        publishState()
    }

    // 发布当前视频状态，并启动必要的进度和缓冲任务。
    private fun publishState() {
        val status = active()?.state?.value ?: return
        mutableState.value = state.value.copy(
            isPlaying = status.isPlaying,
            wantsPlay = wantsPlay,
            errorCode = status.errorCode,
        )
        val allowed = policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
        // 仅允许播放且无错误时显示缓冲提示。
        updateBuffering(allowed && status.isBuffering && status.errorCode == null)
        // 首次报告输出时记录耗时；输出就绪不等于 Surface 首帧，不能当遮罩条件。
        if (allowed && status.outputVersion > 0 && !outputReported) {
            outputReported = true
            AppLogger.debug(TAG) { "切换输出耗时ms=${(System.nanoTime() - activatedAt) / 1_000_000}" }
        }
        publishProgress()
        // 实际播放且页面可见时才启动采样，避免离屏空转。
        if (status.isPlaying && hostVisible && progressJob?.isActive != true) {
            startProgressSampling()
        }
    }

    // 延迟显示缓冲提示，避免短暂闪烁。
    private fun updateBuffering(isBuffering: Boolean) {
        if (!isBuffering) {
            bufferingJob?.cancel()
            bufferingJob = null
            mutableState.value = state.value.copy(buffering = false)
            return
        }
        // 已经显示或正在等待时不重复计时。
        if (state.value.buffering || bufferingJob?.isActive == true) return
        val current = active()
        bufferingJob = scope.launch {
            delay(bufferingDelayMs)
            // 作品已切换、缓冲已结束或页面/用户已不允许播放时放弃显示。
            if (active() !== current || current?.state?.value?.isBuffering != true ||
                !policy.shouldPlay(hostVisible, primarilyVisible, wantsPlay)
            ) return@launch
            mutableState.value = state.value.copy(buffering = true)
        }
    }

    // 读取当前播放器位置并同步恢复位置。
    private fun publishProgress() {
        active()?.let {
            val p = it.position
            mutableProgress.value = p
            // 留存当前位置，播放器释放后仍能从这里恢复。
            resumePosition = p.positionMs
        }
    }

    // 保存当前作品的位置与用户播放意图。
    private fun checkpoint() {
        // 没有选中作品时不写空书签。
        if (wanted == null) return
        // 有播放器就读实时位置，否则保留暂存值。
        resumePosition = active()?.position?.positionMs ?: resumePosition
        onCheckpoint(PlaybackCheckpoint(wanted?.key, resumePosition, wantsPlay))
    }

    // 归还单个作品的播放器与对应观察资源。
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
        // 让所有尚未完成的旧申请失效。
        generation++
        reconcileJob?.cancel()
        reconcileJob = null
        stopTimers()
        // 复制集合后逐个归还，避免遍历时修改集合。
        players.values.toList().forEach(::recycle)
        pool?.release()
        pool = null
    }

    // 保存位置、取消延迟释放并立即归还全部租约，与离屏后的延迟释放相对。
    private fun releasePlayers() {
        checkpoint()
        teardownJob?.cancel()
        teardownJob = null
        releaseLeases()
        mutableState.value = state.value.copy(isPlaying = false, buffering = false)
    }

    // 复用现有池，没有时按需创建；返回空表示创建期间已过时或离屏，调用方需放弃本轮分配。
    private suspend fun reuseOrCreatePool(ticket: Long): PlayerPool? {
        val currentPool = pool ?: factory.createPool(capacity).also { created ->
            // 创建是挂起点，完成后可能已过时或离屏。
            if (ticket != generation || !hostVisible) {
                created.release()
                return null
            }
            pool = created
        }
        return currentPool
    }

    // 启动进度采样任务，当前播放器停止播放后自行退出。
    private fun startProgressSampling() {
        progressJob = scope.launch {
            var ticks = 0
            while (active()?.state?.value?.isPlaying == true) {
                // 定时采样而非逐帧回调，控制进度流频率。
                delay(progressIntervalMs)
                publishProgress()
                // 每 15 个采样（250ms 间隔，约 3.75 秒）落一次书签，降低写盘频率。
                if (++ticks % 15 == 0) checkpoint()
            }
        }
    }

    companion object {
        private const val TAG = "FeedVideoPlayback"
        // 池申请失败时使用的本地错误码，不与底层播放器错误码冲突。
        private const val INITIALIZATION_ERROR = -1
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val BUFFERING_DELAY_MS = 300L
        private const val TEARDOWN_DELAY_MS = 10_000L
    }
}
