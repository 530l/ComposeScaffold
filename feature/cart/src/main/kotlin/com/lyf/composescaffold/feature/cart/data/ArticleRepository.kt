package com.lyf.composescaffold.feature.cart.data

import com.lyf.composescaffold.core.data.network.NetworkError
import com.lyf.composescaffold.core.data.network.NetworkResult
import com.lyf.composescaffold.core.data.network.safeRequest
import com.lyf.composescaffold.core.data.network.toResult
import com.lyf.composescaffold.feature.cart.data.remote.ArticleListApi
import com.lyf.composescaffold.feature.cart.data.remote.WanApiResponse
import com.lyf.composescaffold.feature.cart.data.remote.WanArticleListDto
import com.lyf.composescaffold.feature.cart.domain.Article
import com.lyf.composescaffold.feature.cart.domain.ArticlePage
import com.lyf.composescaffold.feature.cart.domain.ArticleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException

@Singleton
class DefaultArticleRepository @Inject constructor(
    private val api: ArticleListApi,
) : ArticleRepository {
    override suspend fun loadPage(page: Int): Result<ArticlePage> {
        require(page >= FIRST_PAGE) { "页码必须从 $FIRST_PAGE 开始" }
        return safeRequest {
            api.getArticleList(page - FIRST_PAGE)
        }.unwrapWanApiResponse()
            .toResult()
            .mapCatching(WanArticleListDto::toPage)
    }
}

/** 后端包结构属于当前业务；通用网络层只处理 HTTP、连接与反序列化异常。 */
private fun <T : Any> NetworkResult<WanApiResponse<T>>.unwrapWanApiResponse(): NetworkResult<T> =
    when (this) {
        is NetworkResult.Failure -> this
        is NetworkResult.Success -> {
            // 类型参数投影的属性不做 smart-cast，提局部变量后再判空。
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

private fun WanArticleListDto.toPage(): ArticlePage =
    ArticlePage(
        items = datas.map { dto ->
            Article(
                id = dto.id,
                title = dto.title,
                author = dto.author.ifBlank { dto.shareUser },
                chapterName = dto.chapterName.ifBlank { dto.superChapterName },
                link = dto.link,
                niceDate = dto.niceDate,
            )
        },
        // over=true 即最后一页；空页双保险交给 controller 的 End 判定。
        hasMore = !over && datas.isNotEmpty(),
    )

private const val FIRST_PAGE = 1
