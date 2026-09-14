package com.lyf.small.core.design.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 整页状态卡片:列表空态/错误态的居中展示,标题 + 描述 + 可选动作按钮。
 * 文案由调用方传入,适合所有列表页复用。
 */
@Composable
fun FullPageStateCard(
    title: String,
    description: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(bottom = 72.dp),
        contentAlignment = Alignment.Center,
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
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                style = MiuixTheme.textStyles.headline1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.body2,
            )
            if (action != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(text = action)
                }
            }
        }
    }
}
