package com.lyf.small.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Feature 根页面的基础骨架，后续页面可逐步替换卡片内容。 */
@Composable
fun FeaturePlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { SmallTopAppBar(title = title) },
        containerColor = MiuixTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(20.dp),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}
