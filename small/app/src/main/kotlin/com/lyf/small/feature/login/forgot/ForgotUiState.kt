package com.lyf.small.feature.login.forgot

import com.lyf.small.feature.login.components.CodeRequestPhase

/** 忘记密码页唯一状态源：验证与重置两步同页步进，由 ViewModel 以 copy 原子更新。 */
internal data class ForgotUiState(
    val step: ForgotStep = ForgotStep.Verify,
    val account: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val submitting: Boolean = false,
    val codePhase: CodeRequestPhase = CodeRequestPhase.Ready,
) {

    /** 同页两步：先校验账号验证码，再重置新密码。 */
    internal enum class ForgotStep { Verify, Reset }
}
