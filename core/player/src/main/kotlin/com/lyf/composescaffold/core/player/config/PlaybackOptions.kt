package com.lyf.composescaffold.core.player.config

/** 由调用方指定循环和后台播放选项。 */
data class PlaybackOptions(
    /** 循环播放当前媒体。 */
    val repeatOne: Boolean = false,
    /** 通过前台服务支持后台播放；启动失败记录到 backgroundStartFailed。 */
    val backgroundPlayback: Boolean = false,
)
