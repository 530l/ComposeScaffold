package com.lyf.composescaffold.core.data.network

import com.lyf.composescaffold.core.common.config.AppConfig
import com.lyf.composescaffold.core.common.log.AppLogger
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val MAX_RETRIES = 2
private const val MAX_RETRY_DELAY_MILLIS = 2_000L

private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")

fun createJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

fun createOkHttpClient(config: AppConfig): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
    .readTimeout(REQUEST_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
    .writeTimeout(REQUEST_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
    .addInterceptor(RetryInterceptor())
    .apply {
        if (config.enableNetworkLogging) {
            // 商业项目默认不打印正文，避免 token、手机号等敏感信息进入日志。
            addInterceptor(
                HttpLoggingInterceptor { message ->
                    AppLogger.debug("HttpClient") { message }
                }.apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                },
            )
        }
    }
    .build()

fun createRetrofit(
    config: AppConfig,
    okHttpClient: OkHttpClient,
    json: Json,
): Retrofit = Retrofit.Builder()
    .baseUrl(config.apiBaseUrl)
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

/**
 * 幂等重试拦截器：仅对 GET/HEAD/OPTIONS，在 5xx 响应或传输层异常时重试，
 * 指数退避、最多 [MAX_RETRIES] 次；反序列化、参数和业务异常重试不会产生不同结果。
 */
private class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        if (request.method !in IDEMPOTENT_METHODS) {
            return chain.proceed(request)
        }

        var attempt = 0
        var lastException: IOException? = null
        while (attempt <= MAX_RETRIES) {
            if (attempt > 0) {
                // 指数退避：1s、2s…，上限 MAX_RETRY_DELAY_MILLIS
                val delayMillis = minOf(
                    1_000L shl (attempt - 1),
                    MAX_RETRY_DELAY_MILLIS,
                )
                try {
                    Thread.sleep(delayMillis)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("重试等待被中断", interrupted)
                }
            }
            val response = try {
                chain.proceed(request)
            } catch (error: IOException) {
                lastException = error
                attempt++
                continue
            }
            // 5xx 才值得重试；成功与其他错误码直接返回。
            if (response.code in 500..599 && attempt < MAX_RETRIES) {
                response.close()
                attempt++
                continue
            }
            return response
        }
        throw lastException ?: IOException("请求重试耗尽")
    }
}
