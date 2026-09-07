package com.lyf.composescaffold.core.player.playback

/** 播放进度快照，时间字段单位为毫秒。 */
data class PlaybackProgress(
    /** 当前位置。 */
    val positionMs: Long = 0,
    /** 媒体时长（毫秒），未知时为 0。 */
    val durationMs: Long = 0,
    /** 是否支持 Seek。 */
    val seekable: Boolean = false,
    /** 已缓冲到的位置（毫秒）。 */
    val bufferedPositionMs: Long = 0,
)
