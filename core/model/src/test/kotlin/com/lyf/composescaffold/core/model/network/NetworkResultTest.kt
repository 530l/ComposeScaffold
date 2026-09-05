package com.lyf.composescaffold.core.model.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class NetworkResultTest {

    @Test
    fun toResult_success_returnsValue() {
        val result = NetworkResult.Success("hello", 200).toResult()
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("hello")
    }

    @Test
    fun toResult_failure_wrapsInNetworkException() {
        val ioException = IOException("connection reset")
        val result = NetworkResult.Failure(NetworkError.Connectivity(ioException)).toResult()
        assertThat(result.isFailure).isTrue()

        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(NetworkException::class.java)
        val networkException = exception as NetworkException
        assertThat(networkException.error).isEqualTo(NetworkError.Connectivity(ioException))
        assertThat(networkException.message).isEqualTo("网络连接失败")
        assertThat(networkException.cause).isEqualTo(ioException)
    }
}
