package com.lyf.small.data.content.network

import com.lyf.small.core.data.network.ApiCodes
import com.lyf.small.core.data.network.SessionEventManager
import com.lyf.small.data.content.dto.AppError
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.mappers.ApiResponseFailureMapper

/**
 * 全局失败 mapper：把服务端业务码登录失效（errorCode == [ApiCodes.TOKEN_EXPIRED]，HTTP 仍是 200）
 * 上收为全局会话信号。时序依据：Sandwich 先由 ApiEnvelopeMapper 把业务失败转成
 * Failure.Error（payload 为 [AppError]），失败 mapper 在其后执行；其他失败原样透传。
 */
internal class AppLoginExpiredMapper(
    private val sessionEventManager: SessionEventManager,
) : ApiResponseFailureMapper {

    override fun map(apiResponse: ApiResponse.Failure<*>): ApiResponse.Failure<*> {
        val error = (apiResponse as? ApiResponse.Failure.Error)?.payload as? AppError
        if (error?.code == ApiCodes.TOKEN_EXPIRED) {
            sessionEventManager.notifyLoginExpired()
        }
        return apiResponse
    }
}
