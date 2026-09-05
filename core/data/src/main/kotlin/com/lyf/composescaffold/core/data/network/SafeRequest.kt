package com.lyf.composescaffold.core.data.network

import com.lyf.composescaffold.core.model.network.NetworkError
import com.lyf.composescaffold.core.model.network.NetworkResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException

private const val MAX_ERROR_SNIPPET_BYTES = 2_048L

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
