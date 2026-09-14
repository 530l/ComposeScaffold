package com.lyf.small.feature.explore.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyf.small.feature.explore.home.ExploreLoadMoreState
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 列表进入尾部五项时请求下一页，ViewModel 负责最终互斥。 */
@Composable
internal fun ExploreLoadMoreEffect(
    listState: LazyListState,
    loadMoreState: ExploreLoadMoreState,
    isInitializing: Boolean,
    isRefreshing: Boolean,
    onLoadMore: () -> Unit,
) {
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            layoutInfo.totalItemsCount > LOAD_MORE_THRESHOLD &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore, loadMoreState, isInitializing, isRefreshing) {
        if (!shouldLoadMore || loadMoreState != ExploreLoadMoreState.Idle) return@LaunchedEffect
        if (isInitializing || isRefreshing) return@LaunchedEffect
        onLoadMore()
    }
}

/** 探索页分页 Footer，仅展示当前页面实际需要的四种状态。 */
@Composable
internal fun ExploreLoadMoreFooter(
    state: ExploreLoadMoreState,
    loadingText: String,
    failedText: String,
    offlineText: String,
    retryText: String,
    endText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ExploreLoadMoreState.Idle -> Spacer(modifier = modifier.height(4.dp))
        ExploreLoadMoreState.Loading -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfiniteProgressIndicator(
                color = MiuixTheme.colorScheme.primary,
                size = 18.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = loadingText,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                style = MiuixTheme.textStyles.body2,
            )
        }

        is ExploreLoadMoreState.Failed -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.dividerLine,
            )
            Row(
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClick = onRetry,
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.isOffline) offlineText else failedText,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    style = MiuixTheme.textStyles.footnote1,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = retryText,
                    color = MiuixTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.dividerLine,
            )
        }

        ExploreLoadMoreState.End -> Text(
            text = endText,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

private const val LOAD_MORE_THRESHOLD = 5
