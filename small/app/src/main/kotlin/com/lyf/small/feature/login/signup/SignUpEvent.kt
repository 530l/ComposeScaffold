package com.lyf.small.feature.login.signup

/** 注册页一次性事件：成功回调导航，失败带类型交页面翻译成文案。 */
internal sealed interface SignUpEvent {

    /** 注册成功。 */
    data object RegisterSucceeded : SignUpEvent

    /** 业务失败：按 [Message] 在页面层取文案。 */
    data class ShowMessage(val message: Message) : SignUpEvent
}

/** 失败提示类型。 */
internal enum class Message { InvalidCode, PasswordTooShort, Network, Generic }
