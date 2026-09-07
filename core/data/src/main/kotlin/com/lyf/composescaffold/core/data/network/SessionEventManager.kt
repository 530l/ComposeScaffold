package com.lyf.composescaffold.core.data.network

import com.lyf.composescaffold.core.data.storage.CredentialStorageException
import com.lyf.composescaffold.core.data.storage.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 会话可用表示允许读取凭据，并不代表用户已经登录。状态中不携带 Token。 */
sealed interface SessionState {
    data object Available : SessionState
    data class Expired(val credentialCleanupFailed: Boolean) : SessionState
}

/** 会话状态独立于 UI 订阅者；后台失效后，前台重新订阅仍能收到失效状态。 */
@Singleton
class SessionEventManager @Inject constructor(
    private val credentialStore: SecureCredentialStore,
) {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Available)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    /** 同步接口供 OkHttp 工作线程调用；失效后禁止继续发送旧凭据。 */
    @Synchronized
    fun getAuthToken(): String? =
        if (mutableState.value is SessionState.Expired) null else credentialStore.getAuthToken()

    /** 登录成功后调用；凭据持久化失败时保持原会话状态并向调用方报告异常。 */
    @Synchronized
    fun startSession(token: String) {
        require(token.isNotBlank()) { "登录凭据不能为空" }
        credentialStore.saveAuthToken(token)
        mutableState.value = SessionState.Available
    }

    /** 旧请求的 401 不能清除后来保存的新 Token；并发失效只清理一次。 */
    @Synchronized
    fun notifySessionExpired(requestToken: String) {
        if (mutableState.value is SessionState.Expired) return
        try {
            if (credentialStore.getAuthToken() != requestToken) return
            credentialStore.clearAll()
            mutableState.value = SessionState.Expired(credentialCleanupFailed = false)
        } catch (_: CredentialStorageException) {
            // 清理失败仍封锁当前进程内的凭据使用，并保留失败信息供宿主处理。
            mutableState.value = SessionState.Expired(credentialCleanupFailed = true)
        }
    }
}
