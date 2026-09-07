package com.lyf.composescaffold.core.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 使用 Android Keystore 密钥进行 AES-256 GCM 加密，是否硬件保护由设备决定。
 * 保留已有「12 字节 IV + 密文及认证标签」格式；失败显式报告，不自动降级。
 * 同步操作须在工作线程调用，commit 成功后才报告持久化完成。
 */
class AndroidKeyStoreCredentialStore internal constructor(
    private val sharedPreferences: SharedPreferences,
    private val keyProvider: () -> SecretKey,
) : SecureCredentialStore {
    constructor(
        context: Context,
        prefName: String = DEFAULT_PREF_NAME,
        keyAlias: String = DEFAULT_KEY_ALIAS,
    ) : this(
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE),
        { getOrCreateSecretKey(keyAlias) },
    )

    override fun saveAuthToken(token: String) {
        saveCredential(KEY_AUTH_TOKEN, token)
    }

    override fun getAuthToken(): String? = getCredential(KEY_AUTH_TOKEN)

    override fun clearAuthToken() {
        removeCredential(KEY_AUTH_TOKEN)
    }

    @Synchronized
    override fun saveCredential(key: String, value: String): Unit = credentialOperation {
        val encryptedBase64 = encrypt(value)
        check(sharedPreferences.edit().putString(key, encryptedBase64).commit()) { "凭据写入磁盘失败" }
    }

    @Synchronized
    override fun getCredential(key: String): String? = credentialOperation {
        sharedPreferences.getString(key, null)?.let(::decrypt)
    }

    @Synchronized
    override fun removeCredential(key: String): Unit = credentialOperation {
        check(sharedPreferences.edit().remove(key).commit()) { "凭据删除失败" }
    }

    @Synchronized
    override fun clearAll(): Unit = credentialOperation {
        check(sharedPreferences.edit().clear().commit()) { "凭据清理失败" }
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        check(cipher.iv.size == GCM_IV_LENGTH) { "不支持的 GCM IV 长度" }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return SimpleBase64.encode(cipher.iv + ciphertext)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = SimpleBase64.decode(encryptedBase64)
        require(combined.size >= GCM_IV_LENGTH + GCM_TAG_LENGTH_BITS / 8) { "无效的加密凭据数据" }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun <T> credentialOperation(block: () -> T): T = try {
        block()
    } catch (error: Exception) {
        // 异常消息不携带凭据名称或内容，调用方也不得直接记录原始 cause。
        throw CredentialStorageException("安全凭据存储操作失败", error)
    }

    private companion object {
        const val DEFAULT_PREF_NAME = "secure_credentials_store"
        const val DEFAULT_KEY_ALIAS = "compose_scaffold_keystore_alias"
        const val KEY_AUTH_TOKEN = "auth_token_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val AES_KEY_SIZE_BITS = 256

        /** 多个存储实例共享同一别名时，串行创建密钥，避免互相覆盖。 */
        @Synchronized
        fun getOrCreateSecretKey(keyAlias: String): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                val parameterSpec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE_BITS)
                    .build()
                keyGenerator.init(parameterSpec)
                return keyGenerator.generateKey()
            }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                ?: error("KeyStore 密钥条目格式不匹配")
            return entry.secretKey
        }
    }
}

/**
 * 兼容已有密文格式的 Base64 编解码；严格拒绝损坏的输入。
 */
internal object SimpleBase64 {
    private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder((data.size * 4 + 2) / 3)
        var i = 0
        while (i < data.size) {
            val b0 = data[i++].toInt() and 0xFF
            val b1 = if (i < data.size) data[i++].toInt() and 0xFF else -1
            val b2 = if (i < data.size) data[i++].toInt() and 0xFF else -1

            val out0 = b0 ushr 2
            val out1 = ((b0 and 0x03) shl 4) or (if (b1 >= 0) (b1 ushr 4) else 0)
            val out2 = if (b1 >= 0) ((b1 and 0x0F) shl 2) or (if (b2 >= 0) (b2 ushr 6) else 0) else -1
            val out3 = if (b2 >= 0) b2 and 0x3F else -1

            sb.append(TABLE[out0])
            sb.append(TABLE[out1])
            sb.append(if (out2 >= 0) TABLE[out2] else '=')
            sb.append(if (out3 >= 0) TABLE[out3] else '=')
        }
        return sb.toString()
    }

    fun decode(str: String): ByteArray {
        require(str.length % 4 == 0) { "无效的 Base64 长度" }
        val content = str.trimEnd('=')
        require(str.length - content.length <= 2 && content.all { it in TABLE }) { "无效的 Base64 字符" }
        val clean = str
        val output = mutableListOf<Byte>()
        var i = 0
        while (i < clean.length) {
            val c0 = clean[i++]
            val c1 = clean[i++]
            val c2 = clean[i++]
            val c3 = clean[i++]

            val b0 = TABLE.indexOf(c0)
            val b1 = TABLE.indexOf(c1)
            val b2 = if (c2 != '=') TABLE.indexOf(c2) else -1
            val b3 = if (c3 != '=') TABLE.indexOf(c3) else -1

            val out0 = ((b0 shl 2) or (b1 ushr 4)).toByte()
            output.add(out0)

            if (b2 >= 0) {
                val out1 = (((b1 and 0x0F) shl 4) or (b2 ushr 2)).toByte()
                output.add(out1)
            }
            if (b3 >= 0) {
                val out2 = (((b2 and 0x03) shl 6) or b3).toByte()
                output.add(out2)
            }
        }
        return output.toByteArray()
    }
}
