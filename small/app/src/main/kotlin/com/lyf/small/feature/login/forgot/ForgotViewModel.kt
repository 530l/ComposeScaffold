package com.lyf.small.feature.login.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.small.core.infra.network.isConnectivityFailure
import com.lyf.small.data.content.repository.apiErrorCode
import com.lyf.small.data.login.repository.LoginRepository
import com.lyf.small.data.login.repository.MockLoginCodes
import com.lyf.small.feature.login.components.CodeRequestPhase
import com.lyf.small.feature.login.forgot.ForgotUiState.ForgotStep
import com.skydoves.sandwich.ApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 忘记密码页状态源：单一 UiState 原子更新，一次性提示走 events。 */
@HiltViewModel
internal class ForgotViewModel @Inject constructor(
    private val repository: LoginRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ForgotUiState())
    val uiState: StateFlow<ForgotUiState> = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<ForgotEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ForgotEvent> = mutableEvents.asSharedFlow()

    // 验证码倒计时 Job：新一次请求先取消旧倒计时
    private var countdownJob: Job? = null

    fun onAccountChange(account: String) = mutableUiState.update { it.copy(account = account) }

    fun onCodeChange(code: String) = mutableUiState.update { it.copy(code = code) }

    fun onNewPasswordChange(password: String) = mutableUiState.update { it.copy(newPassword = password) }

    fun onConfirmPasswordChange(password: String) = mutableUiState.update { it.copy(confirmPassword = password) }

    /** 发送验证码：倒计时中或提交中忽略，成功后进入 60 秒倒计时。 */
    fun requestCode() {
        val state = mutableUiState.value
        if (state.codePhase is CodeRequestPhase.Counting || state.submitting) return
        viewModelScope.launch {
            when (val response = repository.sendVerificationCode(state.account)) {
                is ApiResponse.Success -> startCountdown()
                is ApiResponse.Failure<*> -> mutableEvents.tryEmit(ForgotEvent.ShowMessage(response.toMessage()))
            }
        }
    }

    /** 按当前步提交：提交中忽略；重置步两次密码不一致时不调仓库。 */
    fun submit() {
        val state = mutableUiState.value
        if (state.submitting) return
        if (state.step == ForgotStep.Reset && state.newPassword != state.confirmPassword) {
            mutableEvents.tryEmit(ForgotEvent.ShowMessage(Message.PasswordMismatch))
            return
        }
        mutableUiState.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val response = when (state.step) {
                ForgotStep.Verify -> repository.verifyResetCode(state.account, state.code)
                ForgotStep.Reset -> repository.resetPassword(state.account, state.newPassword)
            }
            when (response) {
                is ApiResponse.Success -> if (state.step == ForgotStep.Verify) {
                    // 验证通过进入重置步，账号与已填内容保留
                    mutableUiState.update { it.copy(step = ForgotStep.Reset) }
                } else {
                    mutableEvents.tryEmit(ForgotEvent.ResetSucceeded)
                }

                is ApiResponse.Failure<*> -> mutableEvents.tryEmit(ForgotEvent.ShowMessage(response.toMessage()))
            }
            mutableUiState.update { it.copy(submitting = false) }
        }
    }

    /** 60 秒倒计时：每秒刷新剩余秒数，结束后进入「重新获取」。 */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var secondsLeft = CODE_COUNTDOWN_SECONDS
            mutableUiState.update { it.copy(codePhase = CodeRequestPhase.Counting(secondsLeft)) }
            while (secondsLeft > 0) {
                delay(1_000L)
                secondsLeft -= 1
                mutableUiState.update { it.copy(codePhase = CodeRequestPhase.Counting(secondsLeft)) }
            }
            mutableUiState.update { it.copy(codePhase = CodeRequestPhase.ReadyAgain) }
        }
    }

    /** 失败映射：业务码比对 Mock 约定，异常按连通性区分网络与通用失败。 */
    private fun ApiResponse.Failure<*>.toMessage(): Message = when (this) {
        is ApiResponse.Failure.Error -> when (apiErrorCode()) {
            MockLoginCodes.INVALID_CODE -> Message.InvalidCode
            MockLoginCodes.PASSWORD_TOO_SHORT -> Message.PasswordTooShort
            else -> Message.Generic
        }
        is ApiResponse.Failure.Exception -> if (isConnectivityFailure()) Message.Network else Message.Generic
    }

    private companion object {
        const val CODE_COUNTDOWN_SECONDS = 60
    }
}
