package com.lyf.composescaffold.core.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.lyf.composescaffold.core.common.log.AppLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 基于 Android KeyStore (TEE / SE 硬件密钥库) 的敏感凭据安全存储实现。
 *
 * 安全机制：
 * 1. 根密钥保存在 AndroidKeyStore 硬件隔离区中，私钥不可导出；
 * 2. 采用 AES/GCM/NoPadding (256-bit) 认证加密，每次加密生成独立的随机 12 字节 IV；
 * 3. 密文与 IV 一同持久化于隔离 SharedPreferences，防止被窃取或离线篡改；
 * 4. 内置单元测试环境（无硬件 KeyStore 的纯 JVM 环境）安全降级，避免本地测试崩溃。
 */
class AndroidKeyStoreCredentialStore(
    context: Context,
    prefName: String = DEFAULT_PREF_NAME,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : SecureCredentialStore {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    private val isKeyStoreAvailable: Boolean = checkKeyStoreAvailability()
    private val memoryFallbackStore = mutableMapOf<String, String>()

    // 纯 JVM 单测环境下的备用软件密钥
    private val fallbackSoftwareKey: SecretKey by lazy {
        val keyBytes = ByteArray(32) { (it * 31 + 7).toByte() }
        SecretKeySpec(keyBytes, "AES")
    }

    override fun saveAuthToken(token: String) {
        saveCredential(KEY_AUTH_TOKEN, token)
    }

    override fun getAuthToken(): String? = getCredential(KEY_AUTH_TOKEN)

    override fun clearAuthToken() {
        removeCredential(KEY_AUTH_TOKEN)
    }

    override fun saveCredential(key: String, value: String) {
        if (!isKeyStoreAvailable) {
            memoryFallbackStore[key] = value
            return
        }
        try {
            val encryptedBase64 = encrypt(value)
            sharedPreferences.edit().putString(key, encryptedBase64).apply()
        } catch (error: Exception) {
            AppLogger.error(TAG, error) { "加密安全凭据失败: $key" }
        }
    }

    override fun getCredential(key: String): String? {
        if (!isKeyStoreAvailable) {
            return memoryFallbackStore[key]
        }
        val encryptedBase64 = sharedPreferences.getString(key, null) ?: return null
        return try {
            decrypt(encryptedBase64)
        } catch (error: Exception) {
            AppLogger.error(TAG, error) { "解密安全凭据失败: $key" }
            null
        }
    }

    override fun removeCredential(key: String) {
        memoryFallbackStore.remove(key)
        sharedPreferences.edit().remove(key).apply()
    }

    override fun clearAll() {
        memoryFallbackStore.clear()
        sharedPreferences.edit().clear().apply()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return SimpleBase64.encode(combined)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = SimpleBase64.decode(encryptedBase64)
        require(combined.size > GCM_IV_LENGTH) { "无效的加密凭据数据" }
        val iv = ByteArray(GCM_IV_LENGTH)
        val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!isKeyStoreAvailable) return fallbackSoftwareKey
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE,
            )
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
            ?: error("KeyStore 密钥条目格式不匹配: $keyAlias")
        return entry.secretKey
    }

    private companion object {
        const val TAG = "SecureCredentialStore"
        const val DEFAULT_PREF_NAME = "secure_credentials_store"
        const val DEFAULT_KEY_ALIAS = "compose_scaffold_keystore_alias"
        const val KEY_AUTH_TOKEN = "auth_token_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val AES_KEY_SIZE_BITS = 256

        fun checkKeyStoreAvailability(): Boolean = try {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            true
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * 纯 Kotlin 跨平台安全 Base64 实现，避免 Android 与 JVM 单测对 Base64 运行期的差异。
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
        val clean = str.filter { it in TABLE || it == '=' }
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
