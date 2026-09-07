package com.lyf.composescaffold.core.data.repository

import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedOrientation
import com.lyf.composescaffold.core.model.feed.LyricLine
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 静态流媒体样本 JSON 与 LRC 歌词解析引擎（FeedSampleParser）。
 *
 * 【作用与职责】：
 * 纯函数无状态解析工具。负责将 assets 中的原始 JSON 节点转换为结构化的 [FeedItem] 领域实体；
 * 对音频 LRC 歌词进行正则时间戳抽取（毫秒换算）与清洗，对音视频直链进行严格的 HTTPS 安全过滤。
 *
 * 【核心依赖】：
 * - [JsonElement] / [JsonObject]：kotlinx.serialization JSON 元素。
 * - [FeedItem] / [FeedMedia]：输出的领域模型。
 * - [toHttpUrlOrNull]：基于 OkHttp 的安全 URL 结构校验。
 *
 * 【提供功能】：
 * - [parseFeedSample]：解析单条音视频作品 JSON 节点；
 * - [parseLyrics]：将原始 LRC 字符串解析为带毫秒时间戳的有序歌词行集合 [LyricLine]。
 */

/**
 * 将 JSON 元素解析为单条音视频作品条目 [FeedItem]。
 * 过滤非法格式，校验必须字段，强制执行 HTTPS 协议校验。
 *
 * @param element 原始 JSON 元素
 * @return 合法的 FeedItem 实体；数据不完整或协议不安全时返回 null
 */
internal fun parseFeedSample(element: JsonElement): FeedItem? {
    val fields = element as? JsonObject ?: return null // 仅处理 JSON 对象节点
    fun text(name: String) = (fields[name] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty() // 安全提取字符串
    // 强制校验 HTTPS 且禁止包含明文凭据信息
    fun url(name: String) = text(name).toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.username.isEmpty() && it.password.isEmpty()
    }?.toString()
    val type = text("workType") // 作品类型：SONG 或 MV
    val id = text("workId").ifEmpty { text("uuid") } // 优先取 workId，降级取 uuid
    if (id.isEmpty()) return null // 无唯一标识则丢弃
    // 根据作品类型分别构建音频与视频媒体结构
    val media = when (type) {
        "SONG" -> FeedMedia.Music(
            url = url("audioUrl") ?: url("streamAudioUrl") ?: return null, // 优先直链，降级流式链接
            lyrics = parseLyrics(text("lyrics")), // 提取并解析 LRC 歌词
            instrumental = text("subType") in setOf("instrumental", "accompaniment"), // 是否为伴奏纯音乐
        )
        "MV" -> FeedMedia.Mv(
            url = url("videoUrl") ?: return null, // 视频必须具备有效 HTTPS 地址
            orientation = when (text("orientation")) {
                "portrait", "9:16" -> FeedOrientation.PORTRAIT // 竖屏 9:16
                "landscape", "16:9" -> FeedOrientation.LANDSCAPE // 横屏 16:9
                else -> FeedOrientation.UNKNOWN
            },
        )
        else -> return null // 不支持的作品类型忽略
    }
    // 构造具备唯一稳定 key 的条目
    return FeedItem("$type:$id", text("title"), text("userName"), url("imageUrl"), media)
}

/**
 * 解析原始 LRC 歌词文本，抽取 [分:秒.毫秒] 时间戳换算为绝对时间毫秒。
 * 未提供时间戳的歌词统一降级为静态文本展示。
 *
 * @param value 原始 LRC 格式字符串
 * @return 有序歌词行列表
 */
private fun parseLyrics(value: String): List<LyricLine> {
    val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]") // 匹配 [mm:ss.xxx] 时间戳
    val lines = value.lines().flatMap { raw ->
        val matches = timestamp.findAll(raw).toList() // 提取一行中可能出现的多个时间戳标签
        val text = raw.replace(timestamp, "").trim() // 移除时间戳提取纯文本
        if (text.isBlank() || text.matches(Regex("\\[[a-zA-Z]+:.*]"))) {
            emptyList() // 过滤 ID 标签如 [ti:xxx] 或空行
        } else if (matches.isEmpty()) {
            listOf(LyricLine(text)) // 无时间戳的静态行
        } else {
            matches.map { match ->
                // 计算毫秒偏移：分 * 60000 + 秒 * 1000 + 毫秒
                val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLong()
                LyricLine(text, match.groupValues[1].toLong() * 60_000 + match.groupValues[2].toLong() * 1000 + fraction)
            }
        }
    }
    // 全部具备有效时间戳时按时间正序排列；否则统一消除时间戳降级为纯静态行
    return if (lines.all { it.startMs != null }) lines.sortedBy { it.startMs } else lines.map { it.copy(startMs = null) }
}
