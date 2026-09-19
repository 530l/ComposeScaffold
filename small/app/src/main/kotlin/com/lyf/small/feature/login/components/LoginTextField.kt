package com.lyf.small.feature.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 下划线式输入框（设计稿 1注册登录 专用形态）：基于 Miuix TextField 去掉底色与聚焦边框，
 * 底部补 1dp 分割线；占位文案走 label，右侧可挂 trailing（眼睛、清空、获取验证码等）。
 */
@Composable
internal fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            // 下划线形态：底色与聚焦边框全部透明，直角
            insideMargin = DpSize(0.dp, 14.dp),
            backgroundColor = Color.Transparent,
            cornerRadius = 0.dp,
            borderColor = Color.Transparent,
            label = hint,
            labelColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            useLabelAsPlaceholder = true,
            textStyle = MiuixTheme.textStyles.body1.copy(
                color = MiuixTheme.colorScheme.onSurface,
            ),
            keyboardOptions = keyboardOptions,
            singleLine = true,
            visualTransformation = if (password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = trailing,
        )
        // 下划线分隔
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        )
    }
}
