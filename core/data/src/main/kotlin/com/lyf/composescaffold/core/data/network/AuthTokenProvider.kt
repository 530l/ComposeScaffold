package com.lyf.composescaffold.core.data.network

/**
 * 身份认证 Token 提供者契约。
 * 供网络层在发起请求时动态提取当前有效的访问凭证并装配 Header。
 */
fun interface AuthTokenProvider {
    /** 返回当前有效 Token，未登录或已注销时返回 null */
    fun getAuthToken(): String?
}
