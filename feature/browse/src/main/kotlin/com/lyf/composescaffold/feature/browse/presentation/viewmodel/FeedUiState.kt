package com.lyf.composescaffold.feature.browse.presentation.viewmodel

import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableUiState
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedSource

/** 当前模式的列表、加载状态和刷新版本。 */
internal data class FeedUiState(
    override val dataList: List<FeedItem> = emptyList(),
    override val isRefreshing: Boolean = false,
    override val isInitializing: Boolean = true,
    override val loadMoreState: LoadMoreState = LoadMoreState.Idle,
    val source: FeedSource = FeedSource.MUSEAI_PUBLIC,
    val failed: Boolean = false, // 最近一次加载是否失败，用于展示重试入口。
    val revision: Int = 0, // 成功刷新替换列表后递增，通知 UI 回到首项。
) : LoadableUiState<FeedItem, FeedUiState> {
    /** 复制 UI 状态；刷新结束且列表引用变化时递增版本，不深拷贝作品。 */
    override fun copyState(
        dataList: List<FeedItem>,
        isRefreshing: Boolean,
        isInitializing: Boolean,
        loadMoreState: LoadMoreState,
    ): FeedUiState = copy(
        dataList = dataList,
        isRefreshing = isRefreshing,
        isInitializing = isInitializing,
        loadMoreState = loadMoreState,
        // 刷新结束且列表引用已替换时才递增；引用相同只是普通复制，不打扰滚动位置。
        revision = if (this.isRefreshing && !isRefreshing && dataList !== this.dataList) revision + 1 else revision,
    )
}
