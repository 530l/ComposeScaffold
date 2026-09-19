package com.lyf.small.feature.login.components

/** 验证码按钮的三段生命周期；按钮文案与可点状态由页面据此推导。 */
internal sealed interface CodeRequestPhase {

    /** 首次就绪：显示「获取验证码」。 */
    data object Ready : CodeRequestPhase

    /** 发送成功后的倒计时中：显示「已发送(Ns)」且不可点。 */
    data class Counting(val secondsLeft: Int) : CodeRequestPhase

    /** 倒计时结束：显示「重新获取」。 */
    data object ReadyAgain : CodeRequestPhase
}
