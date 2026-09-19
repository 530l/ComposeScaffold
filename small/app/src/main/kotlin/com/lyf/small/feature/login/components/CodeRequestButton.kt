package com.lyf.small.feature.login.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyf.small.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 蓝字描边的验证码按钮：倒计时中或账号未填时置灰不可点。 */
@Composable
internal fun CodeRequestButton(
    phase: CodeRequestPhase,
    accountReady: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counting = phase is CodeRequestPhase.Counting
    val enabled = !counting && accountReady
    val label = when (phase) {
        CodeRequestPhase.Ready -> stringResource(R.string.feature_login_get_code)
        is CodeRequestPhase.Counting -> stringResource(R.string.feature_login_code_countdown, phase.secondsLeft)
        CodeRequestPhase.ReadyAgain -> stringResource(R.string.feature_login_reget_code)
    }
    val color = if (enabled) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceContainerVariant
    }
    Text(
        text = label,
        color = color,
        style = MiuixTheme.textStyles.footnote1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
