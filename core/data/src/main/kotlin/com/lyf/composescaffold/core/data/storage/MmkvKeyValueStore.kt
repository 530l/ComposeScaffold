package com.lyf.composescaffold.core.data.storage

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * MMKV 初始化：必须先于一切 KV 读写，由 Application.onCreate 最先调用。
 * 根目录默认 $(FilesDir)/mmkv；日志级别压到 Warning，避免 Info 噪声进入 release。
 */
object StorageInitializer {
    fun init(context: Context) {
        MMKV.initialize(context, com.tencent.mmkv.MMKVLogLevel.LevelWarning)
    }
}

/**
 * [KeyValueStore] 的 MMKV 实现。
 *
 * - 使用具名实例（mmapID）而非 defaultMMKV()，与库内其他默认存储隔离；
 * - 必须在 [StorageInitializer.init] 完成后才能构造（由启动链路保证：
 *   Application.onCreate 先初始化，早于任何 Hilt 解析）；
 * - MMKV 自身线程安全，实例可在多协程间共享（Hilt 单例）；
 * - 需要加密时走 MMKVConfig 的 cryptKey（密钥须来自平台安全存储，勿硬编码），
 *   届时在 DataModule 中集中调整，业务仍只依赖接口。
 */
class MmkvKeyValueStore(
    mmapID: String = DEFAULT_MMAP_ID,
) : KeyValueStore {
    private val mmkv: MMKV = MMKV.mmkvWithID(mmapID)

    override fun putString(key: String, value: String): Boolean = mmkv.encode(key, value)

    override fun getString(key: String, default: String?): String? =
        mmkv.decodeString(key, default)

    override fun putBoolean(key: String, value: Boolean): Boolean = mmkv.encode(key, value)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        mmkv.decodeBool(key, default)

    override fun putInt(key: String, value: Int): Boolean = mmkv.encode(key, value)

    override fun getInt(key: String, default: Int): Int = mmkv.decodeInt(key, default)

    override fun putLong(key: String, value: Long): Boolean = mmkv.encode(key, value)

    override fun getLong(key: String, default: Long): Long = mmkv.decodeLong(key, default)

    override fun putFloat(key: String, value: Float): Boolean = mmkv.encode(key, value)

    override fun getFloat(key: String, default: Float): Float = mmkv.decodeFloat(key, default)

    override fun putBytes(key: String, value: ByteArray): Boolean = mmkv.encode(key, value)

    override fun getBytes(key: String): ByteArray? = mmkv.decodeBytes(key)

    override fun contains(key: String): Boolean = mmkv.containsKey(key)

    override fun remove(key: String) = mmkv.removeValueForKey(key)

    override fun remove(keys: List<String>) = mmkv.removeValuesForKeys(keys.toTypedArray())

    override fun clearAll() = mmkv.clearAll()

    override fun sync() = mmkv.sync()

    private companion object {
        const val DEFAULT_MMAP_ID = "compose_scaffold_common"
    }
}
