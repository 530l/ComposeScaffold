package com.lyf.composescaffold.core.player.media

/** 媒体类型，决定是否创建视频输出接口。 */
enum class MediaKind {
    /** 纯音频，没有画面。 */
    AUDIO,

    /** 视频，提供 VideoOutput。 */
    VIDEO,
}
