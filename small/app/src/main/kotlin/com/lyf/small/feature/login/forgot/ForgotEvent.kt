package com.lyf.small.feature.login.forgot

/** 忘记密码页一次性事件：重置成功回调导航，失败带类型交页面翻译成文案。 */
internal sealed interface ForgotEvent {

    /** 重置成功。 */
    data object ResetSucceeded : ForgotEvent

    /** 业务失败：按 [Message] 在页面层取文案。 */
    data class ShowMessage(val message: Message) : ForgotEvent
}

/** 提示类别，由页面层翻译成文案。 */
internal enum class Message { InvalidCode, PasswordTooShort, PasswordMismatch, Network, Generic }
