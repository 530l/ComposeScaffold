package com.lyf.composescaffold.core.data.storage

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * [KeyValueStore] 的 MMKV 实现。
 *
 * - 使用具名实例（mmapID）而非 defaultMMKV()，与库内其他默认存储隔离；
 * - 首次由 Hilt 惰性构造时才初始化 MMKV，不为未使用的存储增加冷启动开销；
 * - MMKV 自身线程安全，实例可在多协程间共享（Hilt 单例）；
 * - 本实现不加密，只允许存储非敏感数据，凭证必须使用 Android Keystore 独立实现。
 */
class MmkvKeyValueStore(
    context: Context,
    mmapID: String = DEFAULT_MMAP_ID,
) : KeyValueStore {
    private val mmkv: MMKV = initialize(context, mmapID)

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

        fun initialize(context: Context, mmapID: String): MMKV {
            MMKV.initialize(
                context.applicationContext,
                com.tencent.mmkv.MMKVLogLevel.LevelWarning,
            )
            return MMKV.mmkvWithID(mmapID)
        }
    }
}
