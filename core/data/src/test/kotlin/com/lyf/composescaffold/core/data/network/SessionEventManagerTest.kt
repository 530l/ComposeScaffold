package com.lyf.composescaffold.core.data.network

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.data.storage.CredentialStorageException
import com.lyf.composescaffold.core.data.storage.InMemorySecureCredentialStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionEventManagerTest {
    @Test
    fun backgroundExpirationIsReplayedToLateAndReturningSubscribers() = runTest {
        val store = InMemorySecureCredentialStore().apply {
            saveAuthToken("old")
            saveCredential("refresh_token", "refresh")
        }
        val manager = SessionEventManager(store)
        assertThat(manager.state.first()).isEqualTo(SessionState.Available)
        // 此时没有订阅者；模拟后台 401 后多次返回前台。
        manager.notifySessionExpired("old")
        repeat(2) {
            assertThat(manager.state.first()).isEqualTo(SessionState.Expired(credentialCleanupFailed = false))
        }
        assertThat(manager.getAuthToken()).isNull()
        assertThat(store.getCredential("refresh_token")).isNull()
    }

    @Test
    fun old401DoesNotClearNewLoginAndDuplicate401IsIdempotent() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("old") }
        val manager = SessionEventManager(store)
        manager.startSession("new")
        manager.notifySessionExpired("old")
        assertThat(manager.getAuthToken()).isEqualTo("new")
        assertThat(store.clearCount).isEqualTo(0)
        manager.notifySessionExpired("new")
        manager.notifySessionExpired("new")
        assertThat(store.clearCount).isEqualTo(1)
    }

    @Test
    fun loginResetsExpiredStateOnlyAfterSuccessfulPersistence() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("old") }
        val manager = SessionEventManager(store)
        manager.notifySessionExpired("old")
        store.failWrites = true
        assertThrows(CredentialStorageException::class.java) { manager.startSession("new") }
        assertThat(manager.state.value).isInstanceOf(SessionState.Expired::class.java)
        store.failWrites = false
        manager.startSession("new")
        assertThat(manager.state.value).isEqualTo(SessionState.Available)
        assertThat(manager.getAuthToken()).isEqualTo("new")
    }

    @Test
    fun cleanupFailureIsObservableAndBlocksOldCredentials() {
        val store = InMemorySecureCredentialStore().apply {
            saveAuthToken("old")
            failClear = true
        }
        val manager = SessionEventManager(store)
        manager.notifySessionExpired("old")
        assertThat(manager.state.value).isEqualTo(SessionState.Expired(credentialCleanupFailed = true))
        assertThat(manager.getAuthToken()).isNull()
    }
}
