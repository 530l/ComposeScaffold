package com.lyf.small.data.login.repository

import com.lyf.small.data.login.model.LoginSession
import com.skydoves.sandwich.ApiResponse

/** 注册登录与找回密码的数据契约；后端就绪前由本地 Mock 实现承载。 */
interface LoginRepository {

    /** 向账号发送验证码（登录/注册/找回密码共用一个发送口）。 */
    suspend fun sendVerificationCode(account: String): ApiResponse<Unit>

    /** 密码登录；成功时数据层直接落地会话。 */
    suspend fun loginByPassword(account: String, password: String): ApiResponse<LoginSession>

    /** 验证码登录；成功时数据层直接落地会话。 */
    suspend fun loginByCode(account: String, code: String): ApiResponse<LoginSession>

    /** 注册；账号 + 验证码 + 密码。 */
    suspend fun register(account: String, code: String, password: String): ApiResponse<Unit>

    /** 找回密码第一步：校验账号与验证码。 */
    suspend fun verifyResetCode(account: String, code: String): ApiResponse<Unit>

    /** 找回密码第二步：重置新密码。 */
    suspend fun resetPassword(account: String, newPassword: String): ApiResponse<Unit>
}
