package com.lyf.composescaffold.core.ui.loadmore

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 下拉刷新 + 触底加载的列表容器：
 * - 刷新基于 Material3 官方 [PullToRefreshBox]，保持官方交互；
 * - footer 自动追加在 [content] 之后，feature 不需要自己记得加；
 * - 触底检测内置（[LoadMoreTrigger]），去重交给 [LoadableController]。
 *
 * 刷新状态、触发阈值、指示器与 footer 外观均可由调用方覆盖。
 */
@Composable
fun LoadableLazyColumn(
    isRefreshing: Boolean,
    loadMoreState: LoadMoreState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    pullToRefreshState: PullToRefreshState = rememberPlatformPullToRefreshState(),
    refreshThreshold: Dp = platformPullToRefreshThreshold,
    loadMoreThreshold: Int = DEFAULT_LOAD_MORE_THRESHOLD,
    showEndFooter: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    refreshIndicator: @Composable BoxScope.(PullToRefreshState, Boolean) -> Unit =
        { refreshState, refreshing ->
            DefaultPullToRefreshIndicator(
                state = refreshState,
                isRefreshing = refreshing,
                threshold = refreshThreshold,
            )
        },
    footerContent: @Composable (LoadMoreState) -> Unit = { loadState ->
        LoadMoreFooter(
            state = loadState,
            onRetry = onLoadMore,
            showEndText = showEndFooter,
        )
    },
    content: LazyListScope.() -> Unit,
) {
    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = { refreshIndicator(pullToRefreshState, isRefreshing) },
    ) {
        PlatformListContainer(pullToRefreshState = pullToRefreshState) {
            LazyColumn(
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                horizontalAlignment = horizontalAlignment,
                userScrollEnabled = userScrollEnabled,
            ) {
                content()
                item(key = LOAD_MORE_FOOTER_KEY) { footerContent(loadMoreState) }
            }
        }
        LoadMoreTrigger(
            state = state,
            loadMoreState = loadMoreState,
            onLoadMore = onLoadMore,
            threshold = loadMoreThreshold,
        )
    }
}

@Composable
private fun BoxScope.DefaultPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    threshold: Dp,
) {
    if (platformUsesCompactPullToRefreshIndicator) {
        PullToRefreshDefaults.IndicatorBox(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
            isRefreshing = isRefreshing,
            maxDistance = threshold,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            elevation = 0.dp,
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                CircularProgressIndicator(
                    progress = { state.distanceFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    } else {
        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
            isRefreshing = isRefreshing,
            maxDistance = threshold,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private const val LOAD_MORE_FOOTER_KEY = "load_more_footer"
