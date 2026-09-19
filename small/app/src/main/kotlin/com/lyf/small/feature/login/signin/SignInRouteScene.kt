package com.lyf.small.feature.login.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.small.R
import com.lyf.small.feature.login.components.CodeRequestButton
import com.lyf.small.feature.login.components.LoginMessageHost
import com.lyf.small.feature.login.components.LoginTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.AddSecret
import top.yukonga.miuix.kmp.icon.icons.useful.RemoveSecret
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 登录页：密码 / 验证码双模式，失败提示铺在页面底部。 */
@Composable
internal fun SignInRouteScene(
    onLoginSucceeded: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgot: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordMode = uiState.mode == SignInUiState.SignInMode.Password
    var message by remember { mutableStateOf<String?>(null) }

    val invalidCodeMessage = stringResource(R.string.feature_login_error_code)
    val invalidCredentialsMessage = stringResource(R.string.feature_login_error_credentials)
    val networkMessage = stringResource(R.string.feature_login_error_network)
    val genericMessage = stringResource(R.string.feature_login_error_generic)

    LaunchedEffect(viewModel, invalidCodeMessage, invalidCredentialsMessage, networkMessage, genericMessage) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SignInEvent.LoginSucceeded -> onLoginSucceeded()
                is SignInEvent.ShowMessage -> {
                    message = when (event.message) {
                        Message.InvalidCode -> invalidCodeMessage
                        Message.InvalidCredentials -> invalidCredentialsMessage
                        Message.Network -> networkMessage
                        Message.Generic -> genericMessage
                    }
                    delay(3_000L)
                    message = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 48.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (passwordMode) R.string.feature_login_title_password else R.string.feature_login_title_code,
                    ),
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MiuixTheme.textStyles.title2,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (passwordMode) R.string.feature_login_switch_code else R.string.feature_login_switch_password,
                    ),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.clickable(onClick = viewModel::switchMode),
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            LoginTextField(
                value = uiState.account,
                onValueChange = viewModel::onAccountChange,
                hint = stringResource(R.string.feature_login_hint_account),
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (passwordMode) {
                LoginTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    hint = stringResource(R.string.feature_login_hint_password),
                    password = !uiState.passwordVisible,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailing = {
                        IconButton(onClick = viewModel::togglePasswordVisible) {
                            Icon(
                                imageVector = if (uiState.passwordVisible) {
                                    MiuixIcons.Useful.AddSecret
                                } else {
                                    MiuixIcons.Useful.RemoveSecret
                                },
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            )
                        }
                    },
                )
            } else {
                LoginTextField(
                    value = uiState.code,
                    onValueChange = viewModel::onCodeChange,
                    hint = stringResource(R.string.feature_login_hint_code),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailing = {
                        CodeRequestButton(
                            phase = uiState.codePhase,
                            accountReady = uiState.account.isNotBlank(),
                            onClick = viewModel::requestCode,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Text(
                        text = stringResource(R.string.feature_login_code_notice_prefix),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                    // 后缀「注册。」整体走契约资源并作为跳转入口
                    Text(
                        text = stringResource(R.string.feature_login_code_notice_suffix),
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.clickable(onClick = onNavigateToSignUp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            val fieldsReady = if (passwordMode) {
                uiState.account.isNotBlank() && uiState.password.isNotBlank()
            } else {
                uiState.account.isNotBlank() && uiState.code.isNotBlank()
            }
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = fieldsReady && !uiState.submitting,
                cornerRadius = 0.dp,
                minHeight = 48.dp,
                insideMargin = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.primary,
                    disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
                ),
            ) {
                Text(
                    text = stringResource(R.string.feature_login_action_submit),
                    color = MiuixTheme.colorScheme.onPrimary,
                    style = MiuixTheme.textStyles.button,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (passwordMode) {
                Text(
                    text = stringResource(R.string.feature_login_action_forgot),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onNavigateToForgot),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    text = stringResource(R.string.feature_login_register_prefix),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = stringResource(R.string.feature_login_register_suffix),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.clickable(onClick = onNavigateToSignUp),
                )
            }
        }
        // 提示条铺在页面顶部，避开状态栏
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            LoginMessageHost(message = message)
        }
    }
}
