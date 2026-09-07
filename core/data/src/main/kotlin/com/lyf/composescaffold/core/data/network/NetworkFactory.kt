package com.lyf.composescaffold.core.data.network

import com.lyf.composescaffold.core.common.config.AppConfig
import com.lyf.composescaffold.core.common.log.AppLogger
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 15_000L

internal fun createJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

internal fun createOkHttpClient(
    config: AppConfig,
): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    .readTimeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    .writeTimeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
    // 只保留 OkHttp 对连接失败的内建恢复；HTTP 状态码重试应由具体接口按幂等性决定。
    .retryOnConnectionFailure(true)
    .apply {
        if (config.enableNetworkLogging) {
            addInterceptor { chain ->
                val request = chain.request()
                val startedAt = System.nanoTime()
                try {
                    val response = chain.proceed(request)
                    val durationMillis = (System.nanoTime() - startedAt) / 1_000_000
                    // 不记录 URL、header 与 body，避免账号、token 等敏感数据落入日志。
                    AppLogger.debug("HttpClient") {
                        "${request.method} -> ${response.code} (${durationMillis}ms)"
                    }
                    response
                } catch (error: IOException) {
                    AppLogger.warning("HttpClient") {
                        "${request.method} -> ${error::class.simpleName}"
                    }
                    throw error
                }
            }
        }
    }
    .build()

internal fun createRetrofit(
    config: AppConfig,
    okHttpClient: OkHttpClient,
    json: Json,
): Retrofit = Retrofit.Builder()
    .baseUrl(config.apiBaseUrl)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
