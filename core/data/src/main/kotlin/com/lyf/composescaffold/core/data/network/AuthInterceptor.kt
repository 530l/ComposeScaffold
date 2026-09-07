package com.lyf.composescaffold.core.data.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** 仅向 API 同源请求注入凭据，且只处理本拦截器所用凭据的 401。 */
class AuthInterceptor(
    private val apiBaseUrl: HttpUrl,
    private val tokenProvider: AuthTokenProvider? = null,
    private val sessionEventManager: SessionEventManager? = null,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        // 显式 Authorization 属于调用方，不覆盖，也不据此使应用会话失效。
        val token = if (isApiOrigin(originalRequest.url) && originalRequest.header(HEADER_AUTHORIZATION) == null) {
            tokenProvider?.getAuthToken()?.takeUnless(String::isBlank)
        } else {
            null
        }
        val request = if (token != null) {
            originalRequest.newBuilder().header(HEADER_AUTHORIZATION, "Bearer $token").build()
        } else {
            originalRequest
        }
        val response = chain.proceed(request)
        // 重定向的最终响应也须属于 API 同源，避免外部站点的 401 误伤会话。
        if (response.code == HTTP_UNAUTHORIZED && token != null && isApiOrigin(response.request.url)) {
            sessionEventManager?.notifySessionExpired(token)
        }
        return response
    }

    private fun isApiOrigin(url: HttpUrl): Boolean =
        url.scheme == apiBaseUrl.scheme && url.host == apiBaseUrl.host && url.port == apiBaseUrl.port

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HTTP_UNAUTHORIZED = 401
    }
}
