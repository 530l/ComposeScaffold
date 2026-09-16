package com.lyf.small.feature.explore.home

import com.lyf.small.data.content.model.Article
import com.lyf.small.data.content.model.Banner

/** 探索页唯一状态源，由 ViewModel 以 copy 原子更新。 */
internal data class ExploreUiState(
    val phase: Phase = Phase.Loading,
    val banners: List<Banner> = emptyList(),
    val articles: List<Article> = emptyList(),
    val isRefreshing: Boolean = false,
    val loadMoreState: ExploreLoadMoreState = ExploreLoadMoreState.Idle,
) {
    /** 整页阶段：首次加载、整页失败、正常展示。 */
    internal enum class Phase { Loading, Error, Idle }
}

/** 分页 Footer 的四种状态。 */
internal sealed interface ExploreLoadMoreState {
    data object Idle : ExploreLoadMoreState
    data object Loading : ExploreLoadMoreState
    data class Failed(val isOffline: Boolean) : ExploreLoadMoreState
    data object End : ExploreLoadMoreState
}
