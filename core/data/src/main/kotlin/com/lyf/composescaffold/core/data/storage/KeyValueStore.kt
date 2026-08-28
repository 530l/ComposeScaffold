package com.lyf.composescaffold.core.data.storage

/**
 * 应用统一键值存储接口。
 *
 * 业务代码只通过 Hilt 注入本接口，不直接依赖 MMKV：
 * - 测试可换成内存版 Fake（MMKV 是 native 实现，JVM 单测跑不了，见 AGENTS.md）；
 * - 将来若迁移到其他 KV 实现，业务零改动。
 *
 * key 约定：各业务模块用常量对象集中声明，避免裸字符串散落在调用点：
 * ```
 * object DisplayKeys { const val DARK_MODE = "display_dark_mode" }
 * ```
 *
 * 本接口默认实现是未加密 MMKV，只能存放偏好设置、缓存标记等非敏感数据。
 * token、密码、支付凭证和个人敏感信息必须使用基于 Android Keystore 的独立凭证存储，
 * 禁止为了方便直接写入本接口。
 *
 * 实现约定：线程安全；put 返回 false 表示写入被拒（磁盘满等罕见场景）。
 */
interface KeyValueStore {
    fun putString(key: String, value: String): Boolean
    fun getString(key: String, default: String? = null): String?

    fun putBoolean(key: String, value: Boolean): Boolean
    fun getBoolean(key: String, default: Boolean = false): Boolean

    fun putInt(key: String, value: Int): Boolean
    fun getInt(key: String, default: Int = 0): Int

    fun putLong(key: String, value: Long): Boolean
    fun getLong(key: String, default: Long = 0L): Long

    fun putFloat(key: String, value: Float): Boolean
    fun getFloat(key: String, default: Float = 0f): Float

    fun putBytes(key: String, value: ByteArray): Boolean
    fun getBytes(key: String): ByteArray?

    fun contains(key: String): Boolean

    fun remove(key: String)

    fun remove(keys: List<String>)

    fun clearAll()

    /**
     * 立即落盘。MMKV 写的是 mmap 内存映射文件，崩溃场景下本身已近似持久；
     * 只在对非敏感状态丢失零容忍的节点调用，无需常规调用。
     */
    fun sync()
}
