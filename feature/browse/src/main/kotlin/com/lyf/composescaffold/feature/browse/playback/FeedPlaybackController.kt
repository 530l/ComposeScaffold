package com.lyf.composescaffold.feature.browse.playback

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
import com.lyf.composescaffold.core.model.feed.FeedItem
import kotlinx.coroutines.flow.StateFlow

/** Feed 播放业务聚合状态。 */
internal data class FeedPlaybackState(
    val key: String? = null,
    val isPlaying: Boolean = false,
    val wantsPlay: Boolean = true,
    val buffering: Boolean = false,
    val errorCode: Int? = null,
)

/** 播放检查点，用于状态恢复与跨页面断点续播。 */
internal data class PlaybackCheckpoint(val key: String?,
                                       val positionMs: Long,
                                       val wantsPlay: Boolean)

/** 控制栏所需的最小操作命令契约，不暴露生命周期与底层播放器。 */
internal interface PlaybackCommands {
    fun toggle()
    fun seekTo(positionMs: Long)
    fun retry()
}

/** Feed 信息流播放会话核心顶层契约。 */
internal interface FeedPlaybackController : PlaybackCommands {
    val state: StateFlow<FeedPlaybackState>
    val progress: StateFlow<PlaybackProgress>
    val outputs: StateFlow<Map<String, VideoOutput>>

    /** 相同作品重复选中不重启；切换新作品默认从头播放。 */
    fun select(item: FeedItem, positionMs: Long = 0, playWhenReady: Boolean = true)

    fun preload(item: FeedItem?)

    fun setHostVisible(visible: Boolean)

    /** 滑动过程中旧页仍是 settledPage，但可能已不占主要可见区域。 */
    fun setPrimarilyVisible(visible: Boolean)

    fun reset()

    /** 保存检查点并归还页面资源；全局音乐可继续播放，会话允许重新绑定。 */
    fun close()
}
