package com.lyf.composescaffold.core.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException

private const val MAX_ERROR_SNIPPET_BYTES = 2_048L

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

/**
 * 网络请求的统一安全边界：保留协程取消语义，并把异常转换为可处理的错误模型。
 */
suspend fun <T : Any> safeRequest(
    request: suspend () -> Response<T>,
): NetworkResult<T> = try {
    val response = request()
    if (response.isSuccessful) {
        val body = response.body()
            ?: throw SerializationException("成功响应缺少 body")
        NetworkResult.Success(body, response.code())
    } else {
        NetworkResult.Failure(
            NetworkError.Http(
                statusCode = response.code(),
                responseSnippet = response.errorBody()?.readBoundedSnippet(),
            ),
        )
    }
} catch (error: CancellationException) {
    throw error
} catch (error: SerializationException) {
    NetworkResult.Failure(NetworkError.InvalidPayload(error))
} catch (error: IOException) {
    NetworkResult.Failure(NetworkError.Connectivity(error))
} catch (error: Exception) {
    NetworkResult.Failure(NetworkError.Unknown(error))
}

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

/** 只读取错误正文开头，避免异常服务端响应导致整段正文进入内存。 */
private fun okhttp3.ResponseBody.readBoundedSnippet(): String? = try {
    use { body ->
        val source = body.source()
        source.request(MAX_ERROR_SNIPPET_BYTES)
        val byteCount = minOf(source.buffer.size, MAX_ERROR_SNIPPET_BYTES)
        source.buffer.clone().readUtf8(byteCount).ifBlank { null }
    }
} catch (_: IOException) {
    null
}
