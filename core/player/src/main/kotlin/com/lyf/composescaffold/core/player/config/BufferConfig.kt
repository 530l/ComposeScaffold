package com.lyf.composescaffold.core.player.config

/** 单个播放器的缓冲目标，不代表进程内存上限。 */
data class BufferConfig(
    /** 最小缓冲时长目标（毫秒），可能受字节阈值限制。 */
    val minBufferMs: Int = 2_000,
    /** 最大缓冲时长目标（毫秒）。 */
    val maxBufferMs: Int = 10_000,
    /** 起播所需的缓冲时长目标（毫秒）。 */
    val startBufferMs: Int = 500,
    /** 缓冲中断后恢复播放的时长目标（毫秒）。 */
    val rebufferMs: Int = 1_000,
    /** 目标缓冲字节数，不是严格内存上限。 */
    val targetBytes: Int = 16 * 1024 * 1024,
) {
    init {
        // 最小缓冲目标不能低于起播和恢复播放的阈值。
        require(startBufferMs >= 0 && rebufferMs >= 0)
        require(minBufferMs >= startBufferMs && minBufferMs >= rebufferMs)
        require(maxBufferMs >= minBufferMs)
        require(targetBytes > 0)
    }
}
