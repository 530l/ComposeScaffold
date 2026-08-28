package com.lyf.composescaffold.feature.cart.domain

/** 文章数据契约；presentation 只依赖 domain，不感知远端协议与实现。 */
interface ArticleRepository {
    /** [page] 为 1 基页码，具体数据源的页码规则由实现负责转换。 */
    suspend fun loadPage(page: Int): Result<ArticlePage>
}
