package com.lyf.small.feature.login.signup

import com.lyf.small.feature.login.components.CodeRequestPhase

/** 注册页唯一状态源，由 ViewModel 以 copy 原子更新。 */
internal data class SignUpUiState(
    val account: String = "",
    val code: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val codePhase: CodeRequestPhase = CodeRequestPhase.Ready,
)
