package com.lyf.small.feature.login.forgot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.small.R
import com.lyf.small.feature.login.components.CodeRequestButton
import com.lyf.small.feature.login.components.LoginMessageHost
import com.lyf.small.feature.login.components.LoginTextField
import com.lyf.small.feature.login.forgot.ForgotUiState.ForgotStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 忘记密码页：验证与重置两步同页步进，成功提示后回调返回。 */
@Composable
internal fun ForgotRouteScene(
    onBack: () -> Unit,
    onResetSucceeded: () -> Unit,
    viewModel: ForgotViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }

    // 事件文案统一在页面层翻译
    val resetStep = uiState.step == ForgotStep.Reset
    val successMessage = stringResource(R.string.feature_forgot_success)
    val invalidCodeMessage = stringResource(R.string.feature_login_error_code)
    val passwordLengthMessage = stringResource(R.string.feature_login_error_password_length)
    val passwordMismatchMessage = stringResource(R.string.feature_login_error_password_mismatch)
    val networkMessage = stringResource(R.string.feature_login_error_network)
    val genericMessage = stringResource(R.string.feature_login_error_generic)

    LaunchedEffect(
        viewModel,
        successMessage,
        invalidCodeMessage,
        passwordLengthMessage,
        passwordMismatchMessage,
        networkMessage,
        genericMessage,
    ) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ForgotEvent.ResetSucceeded -> {
                    message = successMessage
                    delay(800L)
                    onResetSucceeded()
                }

                is ForgotEvent.ShowMessage -> {
                    message = when (event.message) {
                        Message.InvalidCode -> invalidCodeMessage
                        Message.PasswordTooShort -> passwordLengthMessage
                        Message.PasswordMismatch -> passwordMismatchMessage
                        Message.Network -> networkMessage
                        Message.Generic -> genericMessage
                    }
                    delay(3_000L)
                    message = null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(
                    if (resetStep) R.string.feature_forgot_title_reset else R.string.feature_forgot_title,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Back,
                            contentDescription = stringResource(R.string.back),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        containerColor = MiuixTheme.colorScheme.surfaceVariant,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp),
            ) {
                if (resetStep) {
                    LoginTextField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        hint = stringResource(R.string.feature_login_hint_new_password),
                        password = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LoginTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        hint = stringResource(R.string.feature_login_hint_confirm_password),
                        password = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                } else {
                    LoginTextField(
                        value = uiState.account,
                        onValueChange = viewModel::onAccountChange,
                        hint = stringResource(R.string.feature_login_hint_account),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                }
                Spacer(modifier = Modifier.height(48.dp))
                // 按钮可用性由当前步必填项与提交态直接推导
                val fieldsReady = if (resetStep) {
                    uiState.newPassword.isNotBlank() && uiState.confirmPassword.isNotBlank()
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
                        text = stringResource(R.string.feature_forgot_action),
                        color = MiuixTheme.colorScheme.onPrimary,
                        style = MiuixTheme.textStyles.button,
                    )
                }
            }
            // 提示条铺在页面顶部（顶栏下方）
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                LoginMessageHost(message = message)
            }
        }
    }
}
