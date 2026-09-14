package com.lyf.small.core.data.storage

/**
 * 敏感凭证专用安全存储接口。
 *
 * 与 [KeyValueStore] 的职责边界强隔离：
 * - [KeyValueStore]（MMKV 实现）专用于非敏感数据（主题偏好、UI 滚动位置、界面标记等）；
 * - [SecureCredentialStore] 专用于安全凭据（访问令牌 Token、刷新令牌 RefreshToken、用户鉴权密钥等），
 *   底层使用 Android Keystore 管理密钥，禁止明文写入磁盘，硬件保护能力取决于设备。
 * 所有操作须在工作线程调用；存储失败抛出 [CredentialStorageException]，读取缺失键才返回 null。
 */
interface SecureCredentialStore {

    /** 保存登录/访问 Token */
    fun saveAuthToken(token: String)

    /** 获取登录/访问 Token */
    fun getAuthToken(): String?

    /** 清除登录/访问 Token */
    fun clearAuthToken()

    /** 保存命名安全凭证（如 RefreshToken、支付私钥等） */
    fun saveCredential(key: String, value: String)

    /** 获取命名安全凭证 */
    fun getCredential(key: String): String?

    /** 移除指定安全凭证 */
    fun removeCredential(key: String)

    /** 清空所有安全凭证（用于登出或账号注销） */
    fun clearAll()
}

/**
 * 可由网络和业务调用方识别的存储失败，不得将其当作保存成功或未登录。
 * 刻意不继承 IOException:凭据存储故障不是网络连接问题,
 * 混入 IO 层级会被 safeRequest 误分类为 Connectivity。
 */
class CredentialStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
