package com.lyf.small.data.login.repository

/** Mock 阶段私有的业务约定：固定验证码、密码下限与错误码；接真实后端时整体移除。 */
internal object MockLoginCodes {

    /** 万能验证码，供联调与演示错误态使用。 */
    const val VERIFICATION_CODE = "123456"

    const val MIN_PASSWORD_LENGTH = 6

    /** 验证码不正确。 */
    const val INVALID_CODE = 4001

    /** 账号或密码不正确。 */
    const val INVALID_CREDENTIALS = 4002

    /** 密码不满足长度要求。 */
    const val PASSWORD_TOO_SHORT = 4003
}
