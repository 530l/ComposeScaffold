package com.lyf.composescaffold.core.data.storage

/** 仅存在于测试源码中的显式替身。 */
internal class InMemorySecureCredentialStore : SecureCredentialStore {
    private val values = mutableMapOf<String, String>()
    var failWrites = false
    var failClear = false
    var clearCount = 0

    override fun saveAuthToken(token: String) = saveCredential("auth_token", token)
    override fun getAuthToken(): String? = getCredential("auth_token")
    override fun clearAuthToken() = removeCredential("auth_token")

    override fun saveCredential(key: String, value: String) {
        if (failWrites) throw CredentialStorageException("模拟写入失败")
        values[key] = value
    }

    override fun getCredential(key: String): String? = values[key]

    override fun removeCredential(key: String) {
        values.remove(key)
    }

    override fun clearAll() {
        clearCount += 1
        if (failClear) throw CredentialStorageException("模拟清理失败")
        values.clear()
    }
}
