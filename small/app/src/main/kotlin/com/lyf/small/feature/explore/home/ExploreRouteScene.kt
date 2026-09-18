package com.lyf.small.feature.explore.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.small.R
import com.lyf.small.data.content.model.Article
import com.lyf.small.feature.explore.home.components.ExploreContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun ExploreRouteScene(
    onArticleClick: (Article) -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshFailedMessage = stringResource(R.string.feature_explore_refresh_failed)
    val refreshOfflineMessage = stringResource(R.string.feature_explore_refresh_offline)
    val requireLoginMessage = stringResource(R.string.feature_explore_require_login)
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel, refreshFailedMessage, refreshOfflineMessage, requireLoginMessage) {
        viewModel.events.collectLatest { event ->
            message = when (event) {
                ExploreEvent.RefreshFailed -> refreshFailedMessage
                ExploreEvent.RefreshOffline -> refreshOfflineMessage
                ExploreEvent.RequireLogin -> requireLoginMessage
            }
            delay(3_000L)
            message = null
        }
    }

    ExploreContent(
        uiState = uiState,
        message = message,
        onRefresh = viewModel::refresh,
        onRetryInitial = viewModel::retryInitial,
        onLoadMore = viewModel::loadMore,
        onArticleClick = onArticleClick,
    )
}
