package com.lyf.small.core.infra.network

import com.lyf.small.core.infra.storage.CredentialStorageException
import com.lyf.small.core.infra.storage.SecureCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEventManagerTest {

    @Test
    fun `notifyLoginExpired 清除本地凭据并进入 Expired`() {
        val store = FakeCredentialStore().apply { token = "t0" }
        val manager = SessionEventManager(store)

        manager.notifyLoginExpired()

        assertTrue(manager.state.value is SessionState.Expired)
        assertNull(store.token)
    }

    @Test
    fun `notifyLoginExpired 未存凭据时无会话可失效保持 Available`() {
        val manager = SessionEventManager(FakeCredentialStore())

        manager.notifyLoginExpired()

        assertEquals(SessionState.Available, manager.state.value)
    }

    @Test
    fun `notifyLoginExpired 清理失败时标记 Expired 且凭据保留`() {
        val store = FakeCredentialStore().apply {
            token = "t0"
            failOnClear = true
        }
        val manager = SessionEventManager(store)

        manager.notifyLoginExpired()

        val state = manager.state.value as SessionState.Expired
        assertTrue(state.credentialCleanupFailed)
        assertEquals("t0", store.token)
    }

    private class FakeCredentialStore : SecureCredentialStore {
        var token: String? = null
        var failOnClear = false

        override fun saveAuthToken(token: String) {
            this.token = token
        }

        override fun getAuthToken(): String? = token

        override fun clearAuthToken() {
            if (failOnClear) throw CredentialStorageException("测试模拟清理失败")
            token = null
        }

        override fun saveCredential(key: String, value: String) = Unit

        override fun getCredential(key: String): String? = null

        override fun removeCredential(key: String) = Unit

        override fun clearAll() = Unit
    }
}
