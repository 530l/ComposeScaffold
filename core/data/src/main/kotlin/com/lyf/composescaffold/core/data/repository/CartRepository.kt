package com.lyf.composescaffold.core.data.repository

import com.lyf.composescaffold.core.data.api.CartApi
import com.lyf.composescaffold.core.data.coroutine.IoDispatcher
import com.lyf.composescaffold.core.data.network.safeRequest
import com.lyf.composescaffold.core.model.article.ArticlePage
import com.lyf.composescaffold.core.model.article.WanApiResponse
import com.lyf.composescaffold.core.model.article.WanArticleListResponse
import com.lyf.composescaffold.core.model.network.NetworkError
import com.lyf.composescaffold.core.model.network.NetworkResult
import com.lyf.composescaffold.core.model.network.toResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

/** 购物车数据仓储契约（按模块前缀命名，扁平存放在 core:data/repository）。 */
interface CartRepository {
    /** [page] 为 1 基页码。 */
    suspend fun loadPage(page: Int): Result<ArticlePage>
}

@Singleton
class DefaultCartRepository @Inject constructor(
    private val api: CartApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CartRepository {
    override suspend fun loadPage(page: Int): Result<ArticlePage> = withContext(ioDispatcher) {
        require(page >= FIRST_PAGE) { "页码必须从 $FIRST_PAGE 开始" }
        safeRequest {
            api.getArticleList(page - FIRST_PAGE)
        }.unwrapWanApiResponse()
            .mapArticlePage()
            .toResult()
    }
}

private fun <T : Any> NetworkResult<WanApiResponse<T>>.unwrapWanApiResponse(): NetworkResult<T> =
    when (this) {
        is NetworkResult.Failure -> this
        is NetworkResult.Success -> {
            val data = value.data
            when {
                value.errorCode != 0 -> NetworkResult.Failure(
                    NetworkError.Api(value.errorCode, value.errorMsg),
                )

                data == null -> NetworkResult.Failure(
                    NetworkError.InvalidPayload(
                        SerializationException("成功响应缺少 data"),
                    ),
                )

                else -> NetworkResult.Success(data, statusCode)
            }
        }
    }

private fun NetworkResult<WanArticleListResponse>.mapArticlePage(): NetworkResult<ArticlePage> =
    when (this) {
        is NetworkResult.Failure -> this
        is NetworkResult.Success -> NetworkResult.Success(
            ArticlePage(
                items = value.datas,
                hasMore = !value.over && value.datas.isNotEmpty(),
            ),
            statusCode,
        )
    }

private const val FIRST_PAGE = 1
