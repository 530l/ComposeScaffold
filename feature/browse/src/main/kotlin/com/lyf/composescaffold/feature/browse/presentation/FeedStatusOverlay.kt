package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.R

/** Feed 信息流状态遮罩层（全屏加载、空数据占位、错误重试与分页提示）。 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.FeedStatusOverlay(
    state: FeedUiState,
    mode: FeedMode,
    restoring: Boolean,
    settledPage: Int,
    interactionError: Boolean,
    onIntent: (FeedIntent) -> Unit,
) {
    val showBlockingLoading = if (mode == FeedMode.MUSIC) {
        state.isInitializing
    } else {
        state.isInitializing || (restoring && !state.failed && state.dataList.isNotEmpty())
    }

    if (showBlockingLoading) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (state.dataList.isEmpty()) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(if (state.failed) R.string.feed_load_failed else R.string.feed_empty))
            TextButton(onClick = { onIntent(FeedIntent.Retry(mode)) }) {
                Text(
                    stringResource(R.string.feed_retry),
                )
            }
        }
    }
    if (state.isRefreshing) CircularProgressIndicator(
        Modifier
            .align(Alignment.TopCenter)
            .padding(12.dp),
    )
    if (state.failed && state.dataList.isNotEmpty()) {
        TextButton(
            onClick = {
                onIntent(
                    if (state.loadMoreState == LoadMoreState.Failed) FeedIntent.Retry(
                        mode,
                    ) else FeedIntent.Refresh(mode),
                )
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface),
        ) { Text(stringResource(R.string.feed_load_failed_retry)) }
    }
    if (interactionError) {
        TextButton(
            onClick = { onIntent(FeedIntent.DismissInteractionError) },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Text(stringResource(R.string.feed_interaction_failed))
        }
    }
    if (state.loadMoreState == LoadMoreState.Loading || state.loadMoreState == LoadMoreState.End && settledPage == state.dataList.lastIndex) {
        Text(
            stringResource(if (state.loadMoreState == LoadMoreState.Loading) R.string.feed_loading_more else R.string.feed_end),
            Modifier
                .align(Alignment.TopCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
