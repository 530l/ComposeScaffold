package com.lyf.small.core.common.config

import java.net.URI
import java.net.URISyntaxException

enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
}

/**
 * 应用运行配置。由 app 组合根从 BuildConfig 注入，不要把密钥放进源码。
 * apiBaseUrl 不设默认值,保证唯一来源是 BuildConfig 的 buildConfigField,
 * 避免双处定义漂移;环境覆盖时仍必须满足同一安全约束。
 */
data class AppConfig(
    val environment: AppEnvironment = AppEnvironment.DEVELOPMENT,
    val apiBaseUrl: String,
    val enableNetworkLogging: Boolean = false,
) {
    init {
        validateApiBaseUrl(apiBaseUrl)
    }
}

private fun validateApiBaseUrl(value: String) {
    val uri = try {
        URI(value)
    } catch (error: URISyntaxException) {
        throw IllegalArgumentException("apiBaseUrl 不是合法 URL", error)
    }
    require(uri.scheme.equals("https", ignoreCase = true)) { "apiBaseUrl 必须使用 HTTPS" }
    require(!uri.host.isNullOrBlank()) { "apiBaseUrl 必须包含合法主机名" }
    require(uri.rawUserInfo == null) { "apiBaseUrl 禁止包含用户名或密码" }
    require(uri.rawQuery == null && uri.rawFragment == null) {
        "apiBaseUrl 禁止包含 query 或 fragment"
    }
    require(uri.rawPath.orEmpty().endsWith('/')) { "apiBaseUrl 必须以 / 结尾" }
}
