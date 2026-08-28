package com.lyf.composescaffold.core.common.config

enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
}

/**
 * 应用运行配置。由 :app 的 Hilt 模块从 BuildConfig 注入，不要把密钥放进源码。
 *
 * 默认 baseUrl 指向 wanandroid 演示接口（脚手架仅使用其文章列表分页接口），
 * 实际项目按 README 环境配置说明注入自己的地址。
 */
data class AppConfig(
    val environment: AppEnvironment = AppEnvironment.DEVELOPMENT,
    val apiBaseUrl: String = "https://www.wanandroid.com/",
    val enableNetworkLogging: Boolean = false,
) {
    init {
        require(apiBaseUrl.startsWith("https://")) { "apiBaseUrl 必须使用 HTTPS" }
        require(apiBaseUrl.endsWith('/')) { "apiBaseUrl 必须以 / 结尾" }
    }
}
