package com.lyf.small.feature.explore.home

/** 探索页用户意图,统一经 [ExploreViewModel.onIntent] 分发。 */
internal sealed interface ExploreIntent {
    data object Refresh : ExploreIntent
    data object RetryInitial : ExploreIntent
    data object LoadMore : ExploreIntent
    data object RetryLoadMore : ExploreIntent
}
