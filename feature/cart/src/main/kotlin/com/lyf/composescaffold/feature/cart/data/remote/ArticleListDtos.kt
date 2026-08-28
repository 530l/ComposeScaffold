package com.lyf.composescaffold.feature.cart.data.remote

import kotlinx.serialization.Serializable

/** wanandroid 专用响应包，留在业务数据源内，避免污染通用网络层。 */
@Serializable
data class WanApiResponse<T>(
    val errorCode: Int = 0,
    val errorMsg: String? = null,
    val data: T? = null,
)

@Serializable
data class WanArticleListDto(
    val curPage: Int = 0,
    val datas: List<WanArticleDto> = emptyList(),
    val over: Boolean = false,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)

@Serializable
data class WanArticleDto(
    val id: Long,
    val title: String = "",
    val superChapterName: String = "",
    val chapterName: String = "",
    val author: String = "",
    val shareUser: String = "",
    val link: String = "",
    val niceDate: String = "",
)
