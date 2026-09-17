package com.lyf.small.data.content.network

import com.lyf.small.core.data.network.ApiCodes
import com.lyf.small.core.data.network.SessionEventManager
import com.lyf.small.core.data.network.SessionState
import com.lyf.small.core.data.storage.SecureCredentialStore
import com.lyf.small.data.content.dto.AppError
import com.skydoves.sandwich.ApiResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AppLoginExpiredMapperTest {

    @Test
    fun `业务码登录失效触发会话过期并原样返回失败`() {
        val manager = managerWithToken()
        val mapper = AppLoginExpiredMapper(manager)
        val failure = ApiResponse.Failure.Error(AppError(ApiCodes.TOKEN_EXPIRED, "登录失效"))

        val mapped = mapper.map(failure)

        assertSame(failure, mapped)
        assertTrue(manager.state.value is SessionState.Expired)
    }

    @Test
    fun `其他业务码不触发会话过期`() {
        val manager = managerWithToken()
        val mapper = AppLoginExpiredMapper(manager)

        mapper.map(ApiResponse.Failure.Error(AppError(-233, "收藏失败")))

        assertEquals(SessionState.Available, manager.state.value)
    }

    @Test
    fun `HTTP 层失败不触发会话过期`() {
        val manager = managerWithToken()
        val mapper = AppLoginExpiredMapper(manager)

        mapper.map(ApiResponse.Failure.Error(Response.success("http-error-payload")))

        assertEquals(SessionState.Available, manager.state.value)
    }

    @Test
    fun `异常失败不触发会话过期`() {
        val manager = managerWithToken()
        val mapper = AppLoginExpiredMapper(manager)

        mapper.map(ApiResponse.Failure.Exception(IllegalStateException("测试异常")))

        assertEquals(SessionState.Available, manager.state.value)
    }

    private fun managerWithToken(): SessionEventManager = SessionEventManager(
        FakeCredentialStore().apply { token = "t0" },
    )

    private class FakeCredentialStore : SecureCredentialStore {
        var token: String? = null

        override fun saveAuthToken(token: String) {
            this.token = token
        }

        override fun getAuthToken(): String? = token

        override fun clearAuthToken() {
            token = null
        }

        override fun saveCredential(key: String, value: String) = Unit

        override fun getCredential(key: String): String? = null

        override fun removeCredential(key: String) = Unit

        override fun clearAll() = Unit
    }
}
