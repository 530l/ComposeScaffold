package com.lyf.composescaffold.core.player.media

/** 单个媒体的描述，不包含列表或队列状态。 */
data class MediaSource(
    /** 稳定的媒体身份；磁盘缓存默认按 URI 区分。 */
    val id: String,
    /** 媒体地址，http/https 或 file/content。 */
    val uri: String,
    /** 媒体类型；视频会创建画面输出接口。 */
    val kind: MediaKind,
    /** 系统媒体通知中的标题。 */
    val title: String = "",
    /** 系统媒体通知中的艺术家。 */
    val artist: String = "",
    /** 系统媒体通知使用的封面地址。 */
    val artworkUri: String? = null,
    /** 初始宽高比，必须为正的有限数。 */
    val initialAspectRatio: Float = 16f / 9f,
    /** 可显式指定 HLS 等类型；null 时根据 URI 推断。 */
    val mimeType: String? = null,
) {
    init {
        // 创建媒体描述时检查参数。
        require(id.isNotBlank())
        require(uri.isNotBlank())
        require(initialAspectRatio.isFinite() && initialAspectRatio > 0)
    }
}
