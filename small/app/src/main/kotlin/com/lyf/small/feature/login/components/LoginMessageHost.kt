package com.lyf.small.feature.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 一次性提示条（灰底白字，设计稿「验证码错误」Toast 形态）。
 * 退出动画期间缓存最后一条消息，避免内容先变空导致卡片瞬时硬切。
 */
@Composable
internal fun LoginMessageHost(message: String?) {
    // 暂存最后一条非空消息：退出动画期间继续渲染它，避免内容先变空导致卡片瞬时硬切。
    val lastMessage = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) lastMessage.value = message
    }
    AnimatedVisibility(visible = message != null) {
        val text = message ?: lastMessage.value
        if (text != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                        contentColor = MiuixTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.widthIn(max = 480.dp),
                ) {
                    Text(
                        text = text,
                        color = MiuixTheme.colorScheme.surface,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
            }
        }
    }
}
