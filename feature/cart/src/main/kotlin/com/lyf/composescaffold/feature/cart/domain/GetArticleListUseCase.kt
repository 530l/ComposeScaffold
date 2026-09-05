package com.lyf.composescaffold.feature.cart.domain

import javax.inject.Inject

/**
 * 业务用例：获取文章列表分页数据。
 *
 * 遵循 Clean Architecture / MAD 领域层规范：
 * - 纯 Kotlin 逻辑，单一职责设计，操作符重载暴露单一公开调用入口 [invoke]；
 * - 隔离 Presentation 层与具体的 Repository 细节，便于在此处扩展跨 Repository 聚合、
 *   业务校验或过滤规则。
 */
internal class GetArticleListUseCase @Inject constructor(
    private val repository: ArticleRepository,
) {
    suspend operator fun invoke(page: Int): Result<ArticlePage> = repository.loadPage(page)
}
