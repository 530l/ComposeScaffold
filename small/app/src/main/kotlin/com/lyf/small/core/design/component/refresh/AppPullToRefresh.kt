package com.lyf.small.core.design.component.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 统一提供 Miuix 原生下拉刷新组件，Feature 不直接管理刷新容器的实现细节。 */
@Composable
fun AppPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    refreshTexts: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshIndicatorPadding = PaddingValues(
        top = contentPadding.calculateTopPadding() + RefreshIndicatorTopSpacing,
    )
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentPadding = refreshIndicatorPadding,
        color = MiuixTheme.colorScheme.primary,
        refreshTexts = refreshTexts,
        refreshTextStyle = MiuixTheme.textStyles.footnote1.copy(
            fontWeight = FontWeight.Medium,
        ),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}

private val RefreshIndicatorTopSpacing = 12.dp
