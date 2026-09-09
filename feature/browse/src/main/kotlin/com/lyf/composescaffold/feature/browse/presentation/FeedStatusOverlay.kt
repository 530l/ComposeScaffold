package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedInteractionFailure
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedInteractionOperation
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedUiState

/** 阻断状态居中、可恢复错误按顺序排列，避免多个提示争用同一位置。 */
@Composable
internal fun BoxScope.FeedStatusOverlay(
    state: FeedUiState,
    mode: FeedMode,
    restoring: Boolean,
    settledPage: Int,
    interactionError: FeedInteractionFailure?,
    onIntent: (FeedIntent) -> Unit,
    onSkipRestoration: () -> Unit = {},
) {
    val blocking = restoring || state.isInitializing || state.dataList.isEmpty()
    if (blocking) {
        FeedRecoveryPanel(state, mode, restoring, onIntent, onSkipRestoration)
    }
    Column(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isRefreshing && !blocking) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.feed_refreshing), style = MaterialTheme.typography.labelMedium)
            }
        }
        // 大厅的追加失败由列表 footer 处理，其他请求失败由页面提示处理。
        if (state.failed && !blocking && (mode != FeedMode.MUSIC || state.loadMoreState != LoadMoreState.Failed)) {
            FeedNotice(
                message = stringResource(R.string.feed_load_failed),
                action = stringResource(R.string.feed_retry),
                onAction = { onIntent(state.retryIntent(mode)) },
            )
        }
        if (interactionError != null && !blocking) {
            val title = state.dataList.firstOrNull { it.key == interactionError.key }?.title
                ?.takeIf { it.isNotBlank() } ?: stringResource(R.string.feed_this_item)
            val message = stringResource(
                when (interactionError.operation) {
                    FeedInteractionOperation.READ -> R.string.feed_interaction_read_failed
                    FeedInteractionOperation.LIKE -> R.string.feed_interaction_like_failed
                    FeedInteractionOperation.SAVE -> R.string.feed_interaction_save_failed
                },
                title,
            )
            FeedNotice(
                message = message,
                action = stringResource(R.string.feed_retry),
                onAction = { onIntent(FeedIntent.RetryInteraction) },
                onDismiss = { onIntent(FeedIntent.DismissInteractionError) },
            )
        }
        if (!blocking && mode != FeedMode.MUSIC && !state.failed && interactionError == null) {
            val hint = when {
                state.loadMoreState == LoadMoreState.Loading -> R.string.feed_loading_more
                state.loadMoreState == LoadMoreState.End && settledPage == state.dataList.lastIndex -> R.string.feed_end
                else -> null
            }
            if (hint != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                    Text(stringResource(hint), Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun FeedRecoveryPanel(
    state: FeedUiState,
    mode: FeedMode,
    restoring: Boolean,
    onIntent: (FeedIntent) -> Unit,
    onSkipRestoration: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            // 恢复面板拦住空白处点击，不能误触下面的播放和互动按钮。
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val loading = state.isInitializing || state.isRefreshing ||
                (restoring && !state.failed && state.loadMoreState != LoadMoreState.End)
            if (loading) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
            Text(
                stringResource(
                    when {
                        restoring -> R.string.feed_restoring_title
                        loading -> R.string.feed_initial_loading
                        state.failed -> R.string.feed_load_failed
                        else -> R.string.feed_empty
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                if (restoring) stringResource(
                    if (state.failed && !loading) R.string.feed_restoring_paused_detail else R.string.feed_restoring_detail,
                    state.dataList.size,
                )
                else stringResource(if (loading) R.string.feed_initial_detail else R.string.feed_empty_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state.failed && !loading) {
                Text(stringResource(R.string.feed_restore_or_load_failed), color = MaterialTheme.colorScheme.error)
            }
            if (!loading) {
                Button(onClick = { onIntent(state.retryIntent(mode)) }) {
                    Text(stringResource(R.string.feed_retry))
                }
            }
            if (restoring && state.dataList.isNotEmpty()) {
                TextButton(onClick = onSkipRestoration) { Text(stringResource(R.string.feed_restore_skip)) }
            }
        }
    }
}

@Composable
private fun FeedNotice(
    message: String,
    action: String,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.align(Alignment.End)) {
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.feed_dismiss)) }
                }
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

private fun FeedUiState.retryIntent(mode: FeedMode): FeedIntent =
    if (dataList.isEmpty() || loadMoreState == LoadMoreState.Failed) FeedIntent.Retry(mode)
    else FeedIntent.Refresh(mode)
