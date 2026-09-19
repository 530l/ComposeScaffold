package com.lyf.small.feature.login.signin

/** 一次性 UI 事件：登录成功导航或失败提示。 */
internal sealed interface SignInEvent {
    data object LoginSucceeded : SignInEvent
    data class ShowMessage(val message: Message) : SignInEvent
}

/** 失败提示类别，由页面层翻译成文案。 */
internal enum class Message { InvalidCode, InvalidCredentials, Network, Generic }
