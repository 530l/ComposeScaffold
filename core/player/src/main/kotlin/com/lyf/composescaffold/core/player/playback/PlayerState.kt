package com.lyf.composescaffold.core.player.playback

/** 播放状态快照，区分播放意图与实际播放状态。 */
data class PlayerState(
    /** 播放正在推进，不代表一定可见或可听。 */
    val isPlaying: Boolean = false,
    /** 播放意图；缓冲或音频焦点抑制时可能与实际状态不同。 */
    val wantsPlay: Boolean = false,
    /** 当前媒体已播放结束。 */
    val ended: Boolean = false,
    /** 正在等待缓冲。 */
    val isBuffering: Boolean = false,
    /** Media3 的错误码，或模块自己定义的；null 表示没出错。 */
    val errorCode: Int? = null,
    /** 后台服务启动失败，此时 errorCode 为 -2。 */
    val backgroundStartFailed: Boolean = false,
    /** 解码初始化或解码失败，供调用方决定是否降级。 */
    val decoderFailed: Boolean = false,
    /** 非本模块直接设置的播放意图变化计数，包括系统和其他播放器的暂停。 */
    val externalIntentVersion: Int = 0,
    /** 输出诊断计数；撤封面应观察 VideoOutput.covered。 */
    val outputVersion: Int = 0,
    /** 媒体时长（毫秒），未知时为 0。 */
    val durationMs: Long = 0,
    /** 是否支持 Seek。 */
    val seekable: Boolean = false,
)
