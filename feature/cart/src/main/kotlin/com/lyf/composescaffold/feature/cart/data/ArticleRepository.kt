package com.lyf.composescaffold.feature.cart.data

import com.lyf.composescaffold.core.data.coroutine.IoDispatcher
import com.lyf.composescaffold.core.data.network.safeRequest
import com.lyf.composescaffold.core.model.network.NetworkError
import com.lyf.composescaffold.core.model.network.NetworkResult
import com.lyf.composescaffold.core.model.network.toResult
import com.lyf.composescaffold.feature.cart.data.remote.ArticleListApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/** 文章数据契约；直接在 data 维护，不引入形式主义的 domain 层。 */
internal interface ArticleRepository {
    /** [page] 为 1 基页码，具体数据源的页码规则由实现负责转换。 */
    suspend fun loadPage(page: Int): Result<ArticlePage>
}

@Singleton
internal class DefaultArticleRepository @Inject constructor(
    private val api: ArticleListApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArticleRepository {
    override suspend fun loadPage(page: Int): Result<ArticlePage> = withContext(ioDispatcher) {
        require(page >= FIRST_PAGE) { "页码必须从 $FIRST_PAGE 开始" }
        safeRequest {
            api.getArticleList(page - FIRST_PAGE)
        }.unwrapWanApiResponse()
            .mapArticlePage()
            .toResult()
    }
}

/** 后端包结构属于当前业务；通用网络层只处理 HTTP、连接与反序列化异常。 */
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

/** 直接复用服务端数据模型构造业务分页载荷，零字段二次映射。 */
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
