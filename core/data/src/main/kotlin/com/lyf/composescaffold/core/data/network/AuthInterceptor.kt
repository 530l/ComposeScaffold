package com.lyf.composescaffold.core.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 商业级身份认证与会话失效拦截器。
 *
 * 功能：
 * 1. 自动注入凭据：当 [tokenProvider] 返回有效 Token 且请求未显式覆盖 Authorization 时，
 *    自动追加 "Authorization: Bearer <token>" 请求头；
 * 2. 会话失效拦截：若响应状态码为 401 Unauthorized，向 [sessionEventManager] 广播会话失效事件。
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider? = null,
    private val sessionEventManager: SessionEventManager? = null,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val token = tokenProvider?.getAuthToken()
        if (!token.isNullOrBlank() && originalRequest.header(HEADER_AUTHORIZATION) == null) {
            requestBuilder.header(HEADER_AUTHORIZATION, "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == HTTP_UNAUTHORIZED) {
            sessionEventManager?.notifySessionExpired()
        }

        return response
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HTTP_UNAUTHORIZED = 401
    }
}
