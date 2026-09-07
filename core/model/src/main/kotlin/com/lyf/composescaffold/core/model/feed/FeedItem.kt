package com.lyf.composescaffold.core.model.feed

/**
 * 流媒体短视频与音乐核心领域实体集（FeedItem）。
 *
 * 【作用与职责】：
 * 规范流媒体作品的数据结构契约。统一表示音乐（包含 LRC 歌词、伴奏标记）与短视频 MV（包含横竖屏方位），
 * 承载唯一稳定的作品标识 [FeedItem.key]、分页实体 [FeedPage] 及分类模式枚举 [FeedMode]。
 *
 * 【核心依赖】：
 * - 纯 Kotlin 标准库（零框架依赖，保持 core:model 的纯洁性）。
 *
 * 【提供功能】：
 * - [FeedMode]：MUSIC/MV/MIXED 分类筛选模式；
 * - [FeedOrientation]：PORTRAIT/LANDSCAPE/UNKNOWN 视频屏幕方向；
 * - [FeedSource]：MUSEAI_PUBLIC/MUSEAI_LOCAL 数据源标识；
 * - [LyricLine]：单行歌词数据（支持可选毫秒时间戳）；
 * - [FeedMedia]：密封媒体类型（Music 音频与 Mv 视频）；
 * - [FeedItem]：作品完整领域实体模型；
 * - [FeedPage]：单页分页模型。
 */

/** 流媒体模式：纯音乐、短视频MV、混合流 */
enum class FeedMode {
    MUSIC, // 纯音乐卡片流
    MV,    // 短视频 MV 流
    MIXED, // 混合作品流
}

/** 视频画面宽高比与方向枚举 */
enum class FeedOrientation {
    PORTRAIT,  // 竖屏 (9:16)
    LANDSCAPE, // 横屏 (16:9)
    UNKNOWN,   // 未知或自适应
}

/** 数据源分类枚举 */
enum class FeedSource {
    MUSEAI_PUBLIC, // 公开远程采样源
    MUSEAI_LOCAL,  // 本地离线样本源
}

/**
 * 单行歌词实体。
 *
 * @param text 歌词文本内容
 * @param startMs 绝对起始毫秒时间戳（null 表示该行无时间戳，仅作为纯文本展示）
 */
data class LyricLine(
    val text: String,
    val startMs: Long? = null,
)

/**
 * 媒体资产密封接口，区分纯音频与视频流。
 */
sealed interface FeedMedia {
    /** 媒体资源网络直链（必须为合法 HTTPS） */
    val url: String

    /**
     * 纯音乐媒体实体。
     *
     * @param url 音频文件直链
     * @param lyrics 关联的解析后歌词行集合
     * @param instrumental 是否为纯器乐/伴奏
     */
    data class Music(
        override val url: String,
        val lyrics: List<LyricLine> = emptyList(),
        val instrumental: Boolean = false,
    ) : FeedMedia

    /**
     * 短视频 MV 媒体实体。
     *
     * @param url 视频文件直链
     * @param orientation 视频画面方向（竖屏/横屏）
     */
    data class Mv(
        override val url: String,
        val orientation: FeedOrientation = FeedOrientation.UNKNOWN,
    ) : FeedMedia
}

/**
 * 流媒体作品卡片完整领域实体。
 *
 * @param key 内容类型与唯一标识的组合（如 "SONG:123"），作为 LazyColumn/VerticalPager 的稳定唯一 key
 * @param title 作品标题
 * @param artist 创作者/艺术家名称
 * @param coverUrl 封面图片 HTTPS 地址
 * @param media 具体的音频或视频媒体载荷
 */
data class FeedItem(
    val key: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val media: FeedMedia,
)

/**
 * 分页数据承载实体。
 *
 * @param items 当前页作品条目列表
 * @param hasMore 是否有更多数据
 * @param source 本页数据源类型
 */
data class FeedPage(
    val items: List<FeedItem>,
    val hasMore: Boolean,
    val source: FeedSource,
)
