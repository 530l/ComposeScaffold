package com.lyf.composescaffold.feature.browse.presentation.mv.playback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.music.toMusicTrack
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 页面播放状态和书签入口；具体播放与切换由协调器执行。 */
@HiltViewModel
internal class FeedPlaybackViewModel @Inject constructor(
    playerFactory: PlayerFactory, // 创建通用播放器池，不暴露 Media3 实现给页面。
    val policy: FeedPlaybackPolicy,
    musicController: MusicPlayerController, // 连接全局音乐，页面不另建音乐播放器。
    private val savedState: SavedStateHandle, // 保存轻量书签，不保存播放器对象。
) : ViewModel(), FeedPlaybackController {
    // 当前绑定的模式；保存书签时用它区分存储位置。
    private var mode: FeedMode? = null

    // 按需读取最新列表，分页后不用重建协调器。
    private var items: () -> List<FeedItem> = { emptyList() }

    // 统一处理音乐和 MV 的播放交接。
    private val coordinator = FeedPlaybackCoordinator(
        // 任务跟随 ViewModel 生命周期取消。
        playerFactory, policy, viewModelScope,
        onCheckpoint = ::rememberCheckpoint,
        musicController = musicController,
    )

    override val state = coordinator.state

    // 进度 250ms 高频更新，单独成流由叶子组件订阅，避免上层 collect 连带重组邻页。
    override val progress = coordinator.progress

    override val outputs = coordinator.outputs

    /** 同模式重新绑定不重置作品；模式切换先保存旧书签。 */
    fun bind(
        mode: FeedMode,
        items: () -> List<FeedItem> = { emptyList() },
    ): FeedPlaybackController {
        // reset 内部会先保存旧模式检查点再清空播放选择。
        if (this.mode != mode) coordinator.reset()
        this.mode = mode
        this.items = items
        // 只把音乐项加入全局音乐队列。
        coordinator.musicQueueProvider = { this.items().mapNotNull { it.toMusicTrack() } }
        // 返回 this：控制会话就是本 ViewModel，多次 bind 不产生第二个会话。
        return this
    }

    fun bookmark(mode: FeedMode): FeedBookmark = FeedBookmark(
        index = savedState["feed.${mode.name}.index"] ?: 0,
        // 作品标识优先于索引，用于列表变化后的定位。
        key = savedState["feed.${mode.name}.key"],
        positionMs = savedState["feed.${mode.name}.position"] ?: 0L,
        playWhenReady = savedState["feed.${mode.name}.play"] ?: true,
    )

    private fun rememberCheckpoint(checkpoint: PlaybackCheckpoint) {
        val mode = mode ?: return
        val index = items().indexOfFirst { it.key == checkpoint.key }
        // key 不在当前列表（列表已刷新或来自其他模式队列）时不落盘，书签只认本列表曲目。
        if (index < 0) return
        // 各模式独立存储键，书签按模式单槽位保存。
        val prefix = "feed.${mode.name}"
        // 索引作为 key 失效时的备用定位。
        savedState["$prefix.index"] = index
        savedState["$prefix.key"] = checkpoint.key
        savedState["$prefix.position"] = checkpoint.positionMs
        savedState["$prefix.play"] = checkpoint.wantsPlay
    }

    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) =
        coordinator.select(item, positionMs, playWhenReady)

    override fun preload(item: FeedItem?) = coordinator.preload(item)

    override fun setHostVisible(visible: Boolean) = coordinator.setHostVisible(visible)

    override fun setPrimarilyVisible(visible: Boolean) = coordinator.setPrimarilyVisible(visible)

    override fun toggle() = coordinator.toggle()

    override fun seekTo(positionMs: Long) = coordinator.seekTo(positionMs)

    override fun retry() = coordinator.retry()

    override fun reset() = coordinator.reset()

    override fun close() = coordinator.close()

    // 页面忘记 close 时的兜底清理。
    override fun onCleared() = close()
}
