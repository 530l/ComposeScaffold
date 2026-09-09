package com.lyf.composescaffold.feature.browse.presentation.viewmodel

import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.music.MusicTrack

/** 页面操作统一通过 Intent 交给 ViewModel；负载自带目标模式或作品标识，路由到对应控制器。 */
internal sealed interface FeedIntent {
    /** 切换视听模式（短视频/音乐/混合）。 */
    data class SelectMode(val mode: FeedMode) : FeedIntent

    data class LoadMore(val mode: FeedMode) : FeedIntent

    data class Refresh(val mode: FeedMode) : FeedIntent

    data class Retry(val mode: FeedMode) : FeedIntent

    data class ToggleLike(val key: String) : FeedIntent

    data class ToggleSave(val key: String) : FeedIntent

    /** 播放单首歌曲并更新播单队列。 */
    data class PlayMusicTrack(
        val track: MusicTrack,
        val queue: List<MusicTrack>, // 供全局音乐切歌使用的已加载曲目。
    ) : FeedIntent

    data class ReadInteraction(val key: String) : FeedIntent

    data object RetryInteraction : FeedIntent

    data object DismissInteractionError : FeedIntent
}
