package com.lyf.small.feature.explore.home

import com.lyf.small.data.content.model.Article
import com.lyf.small.data.content.model.Banner

/** 探索页不可变状态，由 ViewModel 作为唯一状态源。 */
internal data class ExploreUiState(
    val banners: List<Banner> = emptyList(),
    val articles: List<Article> = emptyList(),
    val isInitializing: Boolean = true,
    val hasInitialError: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadMoreState: ExploreLoadMoreState = ExploreLoadMoreState.Idle,
)

internal sealed interface ExploreLoadMoreState {
    data object Idle : ExploreLoadMoreState
    data object Loading : ExploreLoadMoreState
    data class Failed(val isOffline: Boolean) : ExploreLoadMoreState
    data object End : ExploreLoadMoreState
}
