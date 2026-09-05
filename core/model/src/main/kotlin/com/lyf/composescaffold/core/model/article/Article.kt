package com.lyf.composescaffold.core.model.article

import kotlinx.serialization.Serializable

/**
 * wanandroid 文章条目模型（全局通用业务模型，集中在 core:model 供所有模块共享）。
 */
@Serializable
data class Article(
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
data class WanArticleListResponse(
    val curPage: Int = 0,
    val datas: List<Article> = emptyList(),
    val over: Boolean = false,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0,
)

@Serializable
data class WanApiResponse<T>(
    val errorCode: Int = 0,
    val errorMsg: String? = null,
    val data: T? = null,
)

/** 业务分页载荷，直接承载 [Article] 列表。 */
data class ArticlePage(
    val items: List<Article>,
    val hasMore: Boolean,
)
