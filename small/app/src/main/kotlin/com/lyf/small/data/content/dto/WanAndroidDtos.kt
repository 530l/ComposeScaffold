package com.lyf.small.data.content.dto

import kotlinx.serialization.Serializable

/** WanAndroid 通用响应包装，errorCode 非 0 表示业务失败。 */
@Serializable
internal data class WanAndroidEnvelope<T>(
    val data: T? = null,
    val errorCode: Int = 0,
    val errorMsg: String = "",
)

@Serializable
internal data class WanBannerDto(
    val id: Long,
    val title: String = "",
    val imagePath: String = "",
)

@Serializable
internal data class WanArticlePageDto(
    val curPage: Int = 0,
    val datas: List<WanArticleDto> = emptyList(),
    val over: Boolean = true,
    val pageCount: Int = 0,
)

@Serializable
internal data class WanArticleDto(
    val id: Long,
    val title: String = "",
    val author: String = "",
    val shareUser: String = "",
    val chapterName: String = "",
    val superChapterName: String = "",
    val niceDate: String = "",
    val fresh: Boolean = false,
)
