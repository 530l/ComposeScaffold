package com.lyf.small.feature.login.signin

import com.lyf.small.feature.login.components.CodeRequestPhase

/** 登录页唯一状态源，由 ViewModel 以 copy 原子更新。 */
internal data class SignInUiState(
    val mode: SignInMode = SignInMode.Password,
    val account: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val code: String = "",
    val submitting: Boolean = false,
    val codePhase: CodeRequestPhase = CodeRequestPhase.Ready,
) {
    /** 登录模式：密码或验证码。 */
    internal enum class SignInMode { Password, Code }
}
