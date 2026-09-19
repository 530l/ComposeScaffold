package com.lyf.small.data.login.model

/** 登录成功后返回的最小会话信息；Token 不进 UI 状态，由数据层直接落地会话。 */
data class LoginSession(
    val token: String,
    val account: String,
)
