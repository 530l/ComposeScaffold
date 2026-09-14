package com.lyf.small.data.content.model

/** 可供多个 Feature 使用的轮播内容。 */
data class Banner(
    val id: Long,
    val title: String,
    val imageUrl: String,
)

/** 可供多个 Feature 使用的文章内容。 */
data class Article(
    val id: Long,
    val title: String,
    val author: String,
    val category: String,
    val date: String,
    val isFresh: Boolean,
)

/** 内容数据源返回的文章分页结果。 */
data class ArticlePage(
    val articles: List<Article>,
    val hasMore: Boolean,
)
