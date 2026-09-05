package com.lyf.composescaffold.core.model.network

import java.io.IOException

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T, val statusCode: Int) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}

sealed interface NetworkError {
    data class Http(val statusCode: Int, val responseSnippet: String?) : NetworkError
    data class Api(val errorCode: Int, val errorMessage: String?) : NetworkError
    data class Connectivity(val cause: IOException) : NetworkError
    data class InvalidPayload(val cause: Throwable) : NetworkError
    data class Unknown(val cause: Throwable) : NetworkError
}

/** 在异常链中保留结构化网络错误，供日志与上层按需识别。 */
class NetworkException(
    val error: NetworkError,
) : Exception(error.logMessage(), error.causeOrNull())

/** Repository 需要 Kotlin [Result] 时统一在 core 完成错误适配。 */
fun <T> NetworkResult<T>.toResult(): Result<T> = when (this) {
    is NetworkResult.Success -> Result.success(value)
    is NetworkResult.Failure -> Result.failure(NetworkException(error))
}

private fun NetworkError.logMessage(): String = when (this) {
    is NetworkError.Http -> "HTTP $statusCode"
    is NetworkError.Api -> "API errorCode=$errorCode ${errorMessage.orEmpty()}".trimEnd()
    is NetworkError.Connectivity -> "网络连接失败"
    is NetworkError.InvalidPayload -> "响应解析失败"
    is NetworkError.Unknown -> "未知错误"
}

private fun NetworkError.causeOrNull(): Throwable? = when (this) {
    is NetworkError.Http -> null
    is NetworkError.Api -> null
    is NetworkError.Connectivity -> cause
    is NetworkError.InvalidPayload -> cause
    is NetworkError.Unknown -> cause
}
