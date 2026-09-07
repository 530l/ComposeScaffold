package com.lyf.composescaffold.core.player.playback

import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.media.MediaSource

/** 管理播放器的借用和归还；不再使用时释放整个池。 */
interface PlayerPool {
    /** 借用并预备媒体；positionMs 为初始播放位置（毫秒）。 */
    suspend fun acquire(
        source: MediaSource,
        positionMs: Long = 0,
        options: PlaybackOptions = PlaybackOptions(),
    ): Player

    /** 归还播放器，之后不再使用该引用。 */
    fun recycle(player: Player)

    /** 释放整个池，之后不可再申请播放器。 */
    fun release()
}
