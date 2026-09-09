package com.lyf.composescaffold.feature.browse.presentation.mv.playback

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
import com.lyf.composescaffold.core.model.feed.FeedItem
import kotlinx.coroutines.flow.StateFlow

/** 页面展示所需的作品标识与播放状态。 */
internal data class FeedPlaybackState(
    val key: String? = null, // 当前作品标识，尚未选择时为空。
    val isPlaying: Boolean = false, // 实际是否正在出声，缓冲期为 false，不等同于播放意图。
    val wantsPlay: Boolean = true, // 用户播放意图，缓冲期间保持 true。
    val buffering: Boolean = false,
    val errorCode: Int? = null,
)

internal data class PlaybackCheckpoint(val key: String?,
                                       val positionMs: Long,
                                       val wantsPlay: Boolean)

/** 控制栏所需的最小操作命令契约，不暴露生命周期与底层播放器。 */
internal interface PlaybackCommands {
    fun toggle()
    fun seekTo(positionMs: Long)
    fun retry()
}

/** 页面选播、可见性和资源释放的统一入口。 */
internal interface FeedPlaybackController : PlaybackCommands {
    val state: StateFlow<FeedPlaybackState>
    // 进度约 250ms 一刷，单独成流由进度条等叶子订阅；上层 collect 会连带重组相邻页面。
    val progress: StateFlow<PlaybackProgress>
    val outputs: StateFlow<Map<String, VideoOutput>>

    /** 相同作品重复选中不重启；切换新作品默认从头播放。 */
    fun select(item: FeedItem, positionMs: Long = 0, playWhenReady: Boolean = true)

    // 更新相邻预备项，null 表示清空。
    fun preload(item: FeedItem?)

    // 页面级前台条件：宿主 RESUMED 与导航可见性的双条件合成结果。
    fun setHostVisible(visible: Boolean)

    /** 滑动过程中旧页仍是 settledPage，但可能已不占主要可见区域。 */
    fun setPrimarilyVisible(visible: Boolean)

    // 保存旧检查点后清空页面播放选择。
    fun reset()

    /** 保存检查点并归还页面资源；全局音乐可继续播放，会话允许重新绑定。 */
    fun close()
}
