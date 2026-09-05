package com.lyf.composescaffold.core.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局用户会话状态事件管理器。
 *
 * 职责：
 * 当网络底层截获 401 Unauthorized 或 Token 刷新失败信号时，统一广播给应用外壳（如根导航），
 * 触发自动导航至全屏登录页及清理用户本地缓存的流程。
 */
@Singleton
class SessionEventManager @Inject constructor() {

    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 401 会话失效事件流 */
    val sessionExpiredEvents: Flow<Unit> = _sessionExpiredEvents.asSharedFlow()

    /** 触发会话失效通知 */
    fun notifySessionExpired() {
        _sessionExpiredEvents.tryEmit(Unit)
    }
}
