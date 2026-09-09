package com.lyf.composescaffold.feature.browse.presentation.mv.playback

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
    var onCheckpoint: (PlaybackCheckpoint) -> Unit, // 将可恢复的位置和播放意图交回上层保存。
    teardownDelayMs: Long = 10_000L, // MV 页面不可见后的延迟释放时间。
    progressIntervalMs: Long = 250L,
    bufferingDelayMs: Long = 300L, // 短暂缓冲不立即显示加载提示，避免闪烁。
    private val musicController: MusicPlayerController? = null, // 连接全局音乐；未提供时无法播放音乐项。
) : FeedPlaybackController {
    //
    /**
     * 播放新音乐时读取当前队列。 等价于
     * fun interface MusicQueueProvider {
     *     fun get(): List<MusicTrack>
     * }
     * 等价于
     * fun getNumber(provider: () -> Int): Int {
     *     return provider()
     * }
     * val result = getNumber {
     *     100
     * }
     * 相当于定义好能力模板，至于这个数据是怎样获取的，交给外面来处理
     */
    var musicQueueProvider: () -> List<MusicTrack> = { emptyList() }

    private val mutableState = MutableStateFlow(FeedPlaybackState())
    override val state = mutableState.asStateFlow()

    // 进度独立更新，不混进列表状态。
    private val mutableProgress = MutableStateFlow(PlaybackProgress())
    override val progress = mutableProgress.asStateFlow()

    // 把视频池和预备任务交给专门的控制器。
    private val mv = MvPlaybackController(
        // onCheckpoint 是 var，包一层闭包按次取最新回调，直接传引用会固化成旧值。
        factory, policy, scope, { onCheckpoint(it) },
        teardownDelayMs, progressIntervalMs, bufferingDelayMs,
        // MV 真正起播前再次让全局音乐暂停。
        beforePlay = { interruptMusic(MusicPauseReason.MV) },
    )

    // 音乐不需要视频输出，直接暴露 MV 的输出集合。
    override val outputs = mv.outputs

    private var wanted: FeedItem? = null

    private var primarilyVisible = true

    // 页面明确通知可见后才允许 MV 播放。
    private var hostVisible = false

    // 等待音乐支持 Seek 后应用的书签位置。
    private var pendingMusicResumeMs = 0L

    // 只记录本协调器造成、可能需要恢复的暂停。
    private var interruption: MusicInterruption? = null

    private var musicJob: Job? = null

    private var mvStateJob: Job? = null

    private var mvProgressJob: Job? = null

    private val isMusic: Boolean get() = wanted?.media is FeedMedia.Music

    // 选择作品；位置和播放意图通常来自书签。
    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) {
        observeMv()
        if (wanted == item && state.value.key == item.key) return
        checkpointMusic()
        wanted = item
        // 先切换页面标识，实际播放器状态由后续回调补齐。
        mutableState.value = FeedPlaybackState(key = item.key, wantsPlay = playWhenReady)
        if (isMusic) {
            mv.reset()
            startMusic(item, positionMs, playWhenReady)
        } else {
            // 取消旧的音乐观察，避免它继续更新页面。
            musicJob?.cancel()
            musicJob = null
            // 记录为 MV 让位的暂停，避免两种媒体同时发声。
            interruptMusic(MusicPauseReason.MV)
            // 补送当前可见性，MV 控制器据此决定是否值得申请播放器。
            mv.setHostVisible(hostVisible)
            mv.setPrimarilyVisible(primarilyVisible)
            mv.select(item, positionMs, playWhenReady)
            // 立即同步视频状态，不等待下一次流回调。
            mutableState.value = mv.state.value
        }
    }

    override fun preload(item: FeedItem?) {
        if (!isMusic) mv.preload(item)
    }

    // 页面前后台切换或导航可见性变化时回调。
    override fun setHostVisible(visible: Boolean) {
        // 保存可见性，供下次切换 MV 时使用。
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

    // 滑动使当前作品离开主要区域时回调。
    override fun setPrimarilyVisible(visible: Boolean) {
        primarilyVisible = visible
        if (!isMusic) {
            mv.setPrimarilyVisible(visible)
            return
        }
        // 只恢复滑动造成的暂停，MV 抢占的暂停要等切回 MV 才恢复。
        if (visible) resumeInterruptedMusic(MusicPauseReason.SCROLL)
        else interruptMusic(MusicPauseReason.SCROLL)
    }

    override fun toggle() {
        if (!isMusic) return mv.toggle()
        // 手动操作代表用户最新意图，作废尚未消费的自动恢复记录。
        interruption = null
        val item = wanted ?: return
        // 全局队列已不持有该曲目（可能被其他页面切走）时重新提交选播。
        if (musicController?.state?.value?.currentTrack == null) startMusic(item, 0, true)
        else musicController?.toggle()
    }

    override fun seekTo(positionMs: Long) {
        if (isMusic) musicController?.seekTo(positionMs) else mv.seekTo(positionMs)
    }

    override fun retry() {
        if (!isMusic) return mv.retry()
        interruption = null
        // 音乐重试必须整曲重新提交，不能沿用全局播放器的失败现场。
        wanted?.let { startMusic(it, 0, true, restart = true) }
    }

    // 切换模式或刷新时清空当前选择。
    override fun reset() {
        close()
        mv.reset()
        wanted = null
        // 新页面在收到首个手势回调前按主要可见处理。
        primarilyVisible = true
        pendingMusicResumeMs = 0
        mutableState.value = FeedPlaybackState()
        mutableProgress.value = PlaybackProgress()
    }

    // 结束页面绑定；全局音乐不属于页面资源，可继续后台播放。
    override fun close() {
        checkpointMusic()
        // 离开页面只撤销滑动造成的暂停；MV 让位的暂停保留，避免页面外凭空起播。
        if (isMusic) resumeInterruptedMusic(MusicPauseReason.SCROLL)
        hostVisible = false
        musicJob?.cancel()
        musicJob = null
        mvStateJob?.cancel()
        mvStateJob = null
        mvProgressJob?.cancel()
        mvProgressJob = null
        // 不等延迟释放，立即归还 MV 页面资源。
        mv.close()
    }

    // 选择音乐，或在仍为同一首时保留全局播放位置。
    private fun startMusic(
        item: FeedItem,
        positionMs: Long,
        playWhenReady: Boolean,
        restart: Boolean = false, // 显式重试时从头重新提交曲目。
    ) {
        val controller = musicController ?: return
        val track = item.toMusicTrack() ?: return
        val alreadySelected = controller.state.value.currentTrack?.id == item.key
        // 同一首沿用全局进度，新选择才等待恢复书签。
        pendingMusicResumeMs = if (alreadySelected && !restart) 0 else positionMs.coerceAtLeast(0)
        if (!alreadySelected || restart) {
            interruption = null
            // 队列以 lambda 在选播瞬间取最新列表，避免持有过期快照。
            controller.playTrack(track, musicQueueProvider())
            if (!playWhenReady) controller.pause()
            // 同一首要求播放时，先检查是否有可恢复的自动暂停。
        } else if (playWhenReady) {
            // 不覆盖用户手动暂停，只恢复本协调器记录的暂停。
            resumeInterruptedMusic()
        } else {
            interruption = null
            controller.pause()
        }
        // 选播可能发生在滑动途中，目标尚未回到主显示位置时维持滑动暂停。
        if (!primarilyVisible) interruptMusic(MusicPauseReason.SCROLL)
        observeMusic(item)
    }

    // 记录由滑动或 MV 抢占造成的临时暂停。
    private fun interruptMusic(reason: MusicPauseReason) {
        val controller = musicController ?: return
        val snapshot = controller.state.value
        val key = snapshot.currentTrack?.id ?: return
        // 仅暂停仍想播放的音乐；用户自己按下的暂停不属于可恢复范围。
        if (snapshot.wantsPlay) {
            controller.pause()
            // 记下暂停后的意图版本作乐观锁，之后任何用户操作都会推进版本使其失效。
            interruption = MusicInterruption(key, controller.state.value.intentVersion, reason)
            // 暂停已由本协调器持有时只升级原因，例如滑动暂停改为 MV 抢占。
        } else if (interruption?.matches(snapshot) == true) {
            interruption = interruption?.copy(reason = reason)
        }
    }

    // 只恢复曲目和意图版本仍匹配的临时暂停。
    private fun resumeInterruptedMusic(reason: MusicPauseReason? = null) {
        val controller = musicController ?: return
        // 没有自动暂停记录时绝不擅自起播，恢复不等于开始播放。
        val pending = interruption ?: return
        // 指定原因时只恢复该原因的暂停，例如页面退出只回滚滑动暂停。
        if (reason != null && pending.reason != reason) return
        interruption = null
        // 再次校验：期间换歌或用户改过意图就放弃恢复。
        if (pending.matches(controller.state.value)) controller.resume()
    }

    // 分别转发视频状态和进度，避免重复启动收集任务。
    private fun observeMv() {
        if (mvStateJob?.isActive != true) mvStateJob = scope.launch {
            // 当前为视频路径时才让 MV 更新页面状态。
            mv.state.collect { if (!isMusic) mutableState.value = it }
        }
        if (mvProgressJob?.isActive != true) mvProgressJob = scope.launch {
            // 避免迟到的视频进度覆盖音乐进度。
            mv.progress.collect { if (!isMusic) mutableProgress.value = it }
        }
    }

    private fun observeMusic(item: FeedItem) {
        val controller = musicController ?: return
        // 取消旧的音乐观察，避免它继续更新页面。
        musicJob?.cancel()
        musicJob = scope.launch {
            controller.state.collect { snapshot ->
                // 页面已经换作品时忽略旧订阅回调。
                if (wanted != item) return@collect
                // 使用全局实际曲目 ID，供 Pager 同步切歌。
                val key = snapshot.currentTrack?.id
                // 全局已切到其他曲目时丢弃旧书签位置。
                if (key != item.key) pendingMusicResumeMs = 0
                // 对应曲目可 Seek 后才能恢复保存进度。
                if (pendingMusicResumeMs > 0 && snapshot.seekable && key == item.key) {
                    val position = pendingMusicResumeMs
                    pendingMusicResumeMs = 0
                    controller.seekTo(position)
                }
                mutableProgress.value = PlaybackProgress(
                    snapshot.positionMs,
                    snapshot.durationMs,
                    snapshot.seekable,
                    snapshot.bufferedPositionMs,
                )
                mutableState.value = FeedPlaybackState(
                    // 全局曲目尚未就绪时暂用页面目标 key。
                    key = key ?: item.key,
                    // 实际是否正在出声，由全局播放器报告。
                    isPlaying = snapshot.isPlaying,
                    // 临时暂停仍保留用户原本希望播放的意图。
                    wantsPlay = snapshot.wantsPlay || interruption?.matches(snapshot) == true,
                    // 手动暂停时不显示仍在等待播放的缓冲提示。
                    buffering = snapshot.isBuffering && snapshot.wantsPlay,
                    errorCode = snapshot.errorCode,
                )
            }
        }
    }

    // 保存音乐当前位置，MV 的检查点由其控制器报告。
    private fun checkpointMusic() {
        if (!isMusic) return
        val snapshot = musicController?.state?.value ?: return
        onCheckpoint(
            PlaybackCheckpoint(
                // 优先保存实际正在播放的曲目，全局被切走时才退回页面目标。
                snapshot.currentTrack?.id ?: wanted?.key,
                snapshot.positionMs,
                // 临时暂停按原播放意图保存，避免恢复后永久暂停。
                snapshot.wantsPlay || interruption?.matches(snapshot) == true,
            ),
        )
    }

    // 区分滑动暂离和 MV 抢占，决定何时允许恢复。
    private enum class MusicPauseReason { SCROLL, MV }

    /** 保存被暂停的作品、意图版本和原因；用户或系统后来修改意图时旧记录失效。 */
    private data class MusicInterruption(
        val key: String,
        val intentVersion: Long,
        val reason: MusicPauseReason,
    ) {
        fun matches(snapshot: MusicPlaybackSnapshot): Boolean =
            // 同一首、意图版本未推进、且仍处于暂停意图，三者同时成立才算未失效。
            snapshot.currentTrack?.id == key
                && snapshot.intentVersion == intentVersion
                && !snapshot.wantsPlay
    }
}
