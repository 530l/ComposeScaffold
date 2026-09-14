package com.lyf.small.feature.explore.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyf.small.R
import com.lyf.small.core.design.component.FullPageLoading
import com.lyf.small.core.design.component.FullPageStateCard
import com.lyf.small.data.content.model.Article
import com.lyf.small.feature.explore.home.ExploreIntent
import com.lyf.small.feature.explore.home.ExploreUiState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 探索页列表:按状态切换整页占位或轮播 + 文章流,滚动状态与触底加载收在列表内部。 */
@Composable
internal fun ExploreList(
    uiState: ExploreUiState,
    contentPadding: PaddingValues,
    onIntent: (ExploreIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    ExploreLoadMoreEffect(
        listState = listState,
        loadMoreState = uiState.loadMoreState,
        isInitializing = uiState.isInitializing,
        isRefreshing = uiState.isRefreshing,
        onLoadMore = { onIntent(ExploreIntent.LoadMore) },
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
            // start/end 叠加 contentPadding 携带的水平 cutout inset，避免内容伸入遮蔽区。
            start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
            end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            uiState.isInitializing -> item(key = "initial-loading") {
                FullPageLoading(
                    text = stringResource(R.string.feature_explore_initial_loading),
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            uiState.hasInitialError && uiState.articles.isEmpty() -> item(key = "initial-error") {
                FullPageStateCard(
                    title = stringResource(R.string.feature_explore_initial_error_title),
                    description = stringResource(R.string.feature_explore_initial_error_description),
                    action = stringResource(R.string.feature_explore_retry),
                    onAction = { onIntent(ExploreIntent.RetryInitial) },
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            uiState.articles.isEmpty() -> item(key = "empty") {
                FullPageStateCard(
                    title = stringResource(R.string.feature_explore_empty_title),
                    description = stringResource(R.string.feature_explore_empty_description),
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            else -> {
                if (uiState.banners.isNotEmpty()) {
                    item(key = "banners") {
                        ExploreBannerCarousel(banners = uiState.banners)
                    }
                }
                item(key = "article-title") {
                    Text(
                        text = stringResource(R.string.feature_explore_articles_title),
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        fontWeight = FontWeight.SemiBold,
                        style = MiuixTheme.textStyles.title3,
                    )
                }
                items(
                    items = uiState.articles,
                    key = Article::id,
                ) { article ->
                    ExploreArticleCard(article = article)
                }
                item(key = "load-more-footer") {
                    ExploreLoadMoreFooter(
                        state = uiState.loadMoreState,
                        loadingText = stringResource(R.string.feature_explore_loading_more),
                        failedText = stringResource(R.string.feature_explore_load_more_failed),
                        offlineText = stringResource(R.string.feature_explore_load_more_offline),
                        retryText = stringResource(R.string.feature_explore_retry),
                        endText = stringResource(R.string.feature_explore_no_more),
                        onRetry = { onIntent(ExploreIntent.RetryLoadMore) },
                    )
                }
            }
        }
    }
}
