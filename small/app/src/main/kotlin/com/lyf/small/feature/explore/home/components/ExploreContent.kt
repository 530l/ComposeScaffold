package com.lyf.small.feature.explore.home.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lyf.small.R
import com.lyf.small.core.design.component.refresh.AppPullToRefresh
import com.lyf.small.feature.explore.home.ExploreUiState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 探索页组装层：顶栏、刷新容器与一次性提示条；列表内容由 [ExploreList] 提供。 */
@Composable
internal fun ExploreContent(
    uiState: ExploreUiState,
    message: String?,
    onRefresh: () -> Unit,
    onRetryInitial: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SmallTopAppBar(title = stringResource(R.string.feature_explore_title)) },
        snackbarHost = { ExploreMessageHost(message = message) },
        containerColor = MiuixTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
    ) { contentPadding ->
        val refreshTexts = listOf(
            stringResource(R.string.feature_explore_refresh_pull),
            stringResource(R.string.feature_explore_refresh_release),
            stringResource(R.string.feature_explore_refreshing),
            stringResource(R.string.feature_explore_refresh_complete),
        )
        AppPullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            contentPadding = contentPadding,
            refreshTexts = refreshTexts,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(contentPadding),
        ) {
            ExploreList(
                uiState = uiState,
                contentPadding = contentPadding,
                onRetryInitial = onRetryInitial,
                onLoadMore = onLoadMore,
            )
        }
    }
}
