package com.lyf.composescaffold.core.player.playback

import com.lyf.composescaffold.core.player.config.BufferConfig

/** 在主线程创建和操作播放器池；缓存初始化切到 IO。 */
interface PlayerFactory {
    /** 是否为低内存设备，供调用方调整池容量。 */
    val lowRam: Boolean

    /** 创建独立的播放器池，容量和缓冲配置仅作用于该池。 */
    suspend fun createPool(capacity: Int, buffer: BufferConfig = BufferConfig()): PlayerPool
}
