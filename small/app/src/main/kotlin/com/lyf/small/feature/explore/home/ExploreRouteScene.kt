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
import com.lyf.small.feature.explore.home.components.ExploreContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ExploreRouteScene(
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshFailedMessage = stringResource(R.string.feature_explore_refresh_failed)
    val refreshOfflineMessage = stringResource(R.string.feature_explore_refresh_offline)
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel, refreshFailedMessage, refreshOfflineMessage) {
        viewModel.events.collectLatest { event ->
            message = when (event) {
                ExploreEvent.RefreshFailed -> refreshFailedMessage
                ExploreEvent.RefreshOffline -> refreshOfflineMessage
            }
            delay(3_000L.milliseconds)
            message = null
        }
    }

    ExploreContent(
        uiState = uiState,
        message = message,
        onIntent = viewModel::onIntent,
    )
}
