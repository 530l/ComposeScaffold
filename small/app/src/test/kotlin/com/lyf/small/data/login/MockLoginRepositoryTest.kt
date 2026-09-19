package com.lyf.small.data.login

import com.lyf.small.core.infra.network.SessionEventManager
import com.lyf.small.core.infra.storage.CredentialStorageException
import com.lyf.small.core.infra.storage.SecureCredentialStore
import com.lyf.small.data.login.repository.LoginRepository
import com.lyf.small.data.login.repository.MockLoginCodes
import com.lyf.small.data.login.repository.MockLoginRepository
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLoginRepositoryTest {

    @Test
    fun `密码登录成功返回会话并落地登录态`() = runBlocking {
        val manager = SessionEventManager(FakeCredentialStore())
        val repository = repository(manager)

        val response = repository.loginByPassword("user01", "123456")

        assertTrue(response is ApiResponse.Success)
        // 断言 token 落地，与会话默认值区分开
        assertEquals("mock-token-user01", manager.getAuthToken())
    }

    @Test
    fun `密码过短返回业务失败且不落地会话`() = runBlocking {
        val manager = SessionEventManager(FakeCredentialStore())
        val repository = repository(manager)

        val response = repository.loginByPassword("user01", "123")

        assertTrue(response is ApiResponse.Failure)
        assertEquals(null, manager.getAuthToken())
    }

    @Test
    fun `凭据存储失败时登录返回失败而非抛异常`() = runBlocking {
        val manager = SessionEventManager(ThrowingCredentialStore())
        val repository = repository(manager)

        val response = repository.loginByPassword("user01", "123456")

        assertTrue(response is ApiResponse.Failure)
        assertEquals(null, manager.getAuthToken())
    }

    @Test
    fun `验证码登录只认万能验证码`() = runBlocking {
        val manager = SessionEventManager(FakeCredentialStore())
        val repository = repository(manager)

        val wrong = repository.loginByCode("13800138000", "000000")
        val right = repository.loginByCode("13800138000", MockLoginCodes.VERIFICATION_CODE)

        assertTrue(wrong is ApiResponse.Failure)
        assertTrue(right is ApiResponse.Success)
        assertEquals("mock-token-13800138000", manager.getAuthToken())
    }

    @Test
    fun `注册校验验证码与密码长度`() = runBlocking {
        val repository = repository(SessionEventManager(FakeCredentialStore()))

        val wrongCode = repository.register("13800138000", "000000", "123456")
        val shortPassword = repository.register("13800138000", MockLoginCodes.VERIFICATION_CODE, "123")
        val success = repository.register("13800138000", MockLoginCodes.VERIFICATION_CODE, "123456")

        assertTrue(wrongCode is ApiResponse.Failure)
        assertTrue(shortPassword is ApiResponse.Failure)
        assertTrue(success is ApiResponse.Success)
    }

    @Test
    fun `找回密码两步都只认万能验证码与密码下限`() = runBlocking {
        val repository = repository(SessionEventManager(FakeCredentialStore()))

        val wrongVerify = repository.verifyResetCode("13800138000", "000000")
        val verified = repository.verifyResetCode("13800138000", MockLoginCodes.VERIFICATION_CODE)
        val shortReset = repository.resetPassword("13800138000", "123")
        val reset = repository.resetPassword("13800138000", "123456")

        assertTrue(wrongVerify is ApiResponse.Failure)
        assertTrue(verified is ApiResponse.Success)
        assertTrue(shortReset is ApiResponse.Failure)
        assertTrue(reset is ApiResponse.Success)
    }

    private fun repository(manager: SessionEventManager): LoginRepository =
        MockLoginRepository(manager, Dispatchers.Unconfined)

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

        override fun clearAll() {
            token = null
        }
    }

    /** 模拟安全存储写入失败。 */
    private class ThrowingCredentialStore : SecureCredentialStore {
        override fun saveAuthToken(token: String) = throw CredentialStorageException("存储失败")

        override fun getAuthToken(): String? = null

        override fun clearAuthToken() = Unit

        override fun saveCredential(key: String, value: String) = Unit

        override fun getCredential(key: String): String? = null

        override fun removeCredential(key: String) = Unit

        override fun clearAll() = Unit
    }
}
