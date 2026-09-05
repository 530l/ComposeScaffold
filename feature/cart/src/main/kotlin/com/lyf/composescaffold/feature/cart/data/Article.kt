package com.lyf.composescaffold.feature.cart.data

import kotlinx.serialization.Serializable

/**
 * wanandroid 文章条目模型。
 * 直接与服务端 JSON 结构对齐，作为业务实体供 Presentation 层直接使用，消灭无意义的 DTO 手工映射。
 */
@Serializable
internal data class Article(
    val id: Long,
    val title: String = "",
    val chapterName: String = "",
    val superChapterName: String = "",
    val author: String = "",
    val shareUser: String = "",
    val link: String = "",
    val niceDate: String = "",
)

@Serializable
internal data class WanArticleListResponse(
    val curPage: Int = 0,
    val datas: List<Article> = emptyList(),
    val over: Boolean = false,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)

@Serializable
internal data class WanApiResponse<T>(
    val errorCode: Int = 0,
    val errorMsg: String? = null,
    val data: T? = null,
)

/** 业务分页载荷，直接承载 [Article] 列表。 */
internal data class ArticlePage(
    val items: List<Article>,
    val hasMore: Boolean,
)
