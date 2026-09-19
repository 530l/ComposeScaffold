package com.lyf.small.feature.login.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.small.core.infra.network.isConnectivityFailure
import com.lyf.small.data.content.repository.apiErrorCode
import com.lyf.small.data.login.repository.LoginRepository
import com.lyf.small.data.login.repository.MockLoginCodes
import com.lyf.small.feature.login.components.CodeRequestPhase
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

/** 注册页状态源：输入与验证码阶段收敛在单一 UiState，结果经 events 一次性发出。 */
@HiltViewModel
internal class SignUpViewModel @Inject constructor(
    private val repository: LoginRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<SignUpEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SignUpEvent> = mutableEvents.asSharedFlow()

    private var countDownJob: Job? = null

    fun onAccountChange(account: String) = mutableUiState.update { it.copy(account = account) }

    fun onCodeChange(code: String) = mutableUiState.update { it.copy(code = code) }

    fun onPasswordChange(password: String) = mutableUiState.update { it.copy(password = password) }

    /** 请求验证码：成功进入 60 秒倒计时，失败发一次性提示。 */
    fun requestCode() {
        val state = mutableUiState.value
        if (state.account.isBlank() || state.codePhase is CodeRequestPhase.Counting) return
        viewModelScope.launch {
            when (val response = repository.sendVerificationCode(state.account)) {
                is ApiResponse.Failure<*> -> mutableEvents.tryEmit(SignUpEvent.ShowMessage(response.toMessage()))
                is ApiResponse.Success<*> -> startCountDown()
            }
        }
    }

    /** 确认注册：提交中重复调用被忽略；响应后回落可提交态，成功只发事件，失败发提示。 */
    fun submit() {
        val state = mutableUiState.value
        if (state.submitting || state.account.isBlank() || state.code.isBlank() || state.password.isBlank()) return
        mutableUiState.update { it.copy(submitting = true) }
        viewModelScope.launch {
            when (val response = repository.register(state.account, state.code, state.password)) {
                is ApiResponse.Success<*> -> mutableEvents.tryEmit(SignUpEvent.RegisterSucceeded)
                is ApiResponse.Failure<*> -> mutableEvents.tryEmit(SignUpEvent.ShowMessage(response.toMessage()))
            }
            mutableUiState.update { it.copy(submitting = false) }
        }
    }

    private fun startCountDown() {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            // 从 60 逐秒递减，结束后停在「重新获取」
            (COUNTDOWN_SECONDS downTo 1).forEach { seconds ->
                mutableUiState.update { it.copy(codePhase = CodeRequestPhase.Counting(seconds)) }
                delay(1_000L)
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
        const val COUNTDOWN_SECONDS = 60
    }
}
