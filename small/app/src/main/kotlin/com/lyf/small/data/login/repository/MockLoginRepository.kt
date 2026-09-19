package com.lyf.small.data.login.repository

import com.lyf.small.core.infra.coroutine.IoDispatcher
import com.lyf.small.core.infra.network.SessionEventManager
import com.lyf.small.core.infra.storage.CredentialStorageException
import com.lyf.small.data.content.dto.AppError
import com.lyf.small.data.login.model.LoginSession
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 本地假实现：固定延迟模拟网络、验证码只认 [MockLoginCodes.VERIFICATION_CODE]、密码至少 6 位；登录成功直接落地会话。 */
@Singleton
internal class MockLoginRepository @Inject constructor(
    private val sessionEventManager: SessionEventManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : LoginRepository {

    override suspend fun sendVerificationCode(account: String): ApiResponse<Unit> = mock {
        ApiResponse.Success(Unit)
    }

    override suspend fun loginByPassword(account: String, password: String): ApiResponse<LoginSession> = mock {
        if (password.length < MockLoginCodes.MIN_PASSWORD_LENGTH) {
            invalidCredentials()
        } else {
            startMockSession(account)
        }
    }

    override suspend fun loginByCode(account: String, code: String): ApiResponse<LoginSession> = mock {
        if (code != MockLoginCodes.VERIFICATION_CODE) {
            invalidCode()
        } else {
            startMockSession(account)
        }
    }

    override suspend fun register(account: String, code: String, password: String): ApiResponse<Unit> = mock {
        when {
            code != MockLoginCodes.VERIFICATION_CODE -> invalidCode()
            password.length < MockLoginCodes.MIN_PASSWORD_LENGTH -> shortPassword()
            else -> ApiResponse.Success(Unit)
        }
    }

    override suspend fun verifyResetCode(account: String, code: String): ApiResponse<Unit> = mock {
        if (code != MockLoginCodes.VERIFICATION_CODE) invalidCode() else ApiResponse.Success(Unit)
    }

    override suspend fun resetPassword(account: String, newPassword: String): ApiResponse<Unit> = mock {
        if (newPassword.length < MockLoginCodes.MIN_PASSWORD_LENGTH) shortPassword() else ApiResponse.Success(Unit)
    }

    private fun startMockSession(account: String): ApiResponse<LoginSession> {
        val session = LoginSession(token = "mock-token-$account", account = account)
        return try {
            sessionEventManager.startSession(session.token)
            ApiResponse.Success(session)
        } catch (error: CredentialStorageException) {
            // 凭据落地失败按 Sandwich 约定转成失败响应，不让异常穿透数据层
            ApiResponse.Failure.Exception(error)
        }
    }

    private fun invalidCode(): ApiResponse.Failure.Error =
        ApiResponse.Failure.Error(AppError(MockLoginCodes.INVALID_CODE, null))

    private fun invalidCredentials(): ApiResponse.Failure.Error =
        ApiResponse.Failure.Error(AppError(MockLoginCodes.INVALID_CREDENTIALS, null))

    private fun shortPassword(): ApiResponse.Failure.Error =
        ApiResponse.Failure.Error(AppError(MockLoginCodes.PASSWORD_TOO_SHORT, null))

    /** 统一加上模拟网络延迟；失败用 ApiResponse 表达，永不抛异常。 */
    private suspend fun <T> mock(block: () -> ApiResponse<T>): ApiResponse<T> = withContext(dispatcher) {
        delay(LATENCY_MILLIS)
        block()
    }

    private companion object {
        const val LATENCY_MILLIS = 600L
    }
}
