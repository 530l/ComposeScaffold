package com.lyf.small.data.content.repository

import com.lyf.small.core.data.network.safeRequest
import com.lyf.small.core.data.network.NetworkError
import com.lyf.small.core.data.network.NetworkException
import com.lyf.small.core.data.network.NetworkResult
import com.lyf.small.data.content.api.WanAndroidApi
import com.lyf.small.data.content.dto.WanAndroidEnvelope
import com.lyf.small.data.content.dto.WanArticleDto
import com.lyf.small.data.content.dto.WanBannerDto
import com.lyf.small.data.content.mapper.toModel
import com.lyf.small.data.content.model.ArticlePage
import com.lyf.small.data.content.model.Banner
import javax.inject.Inject

/** 多个 Feature 可共同依赖的内容数据契约。 */
interface ContentRepository {
    suspend fun loadBanners(): Result<List<Banner>>

    suspend fun loadArticles(page: Int): Result<ArticlePage>
}

internal class DefaultContentRepository @Inject constructor(
    private val api: WanAndroidApi,
) : ContentRepository {
    override suspend fun loadBanners(): Result<List<Banner>> =
        safeRequest(api::getBanners)
            .unwrapEnvelope()
            .map { banners ->
                banners
                    .distinctBy(WanBannerDto::id)
                    .map(WanBannerDto::toModel)
            }

    override suspend fun loadArticles(page: Int): Result<ArticlePage> =
        safeRequest { api.getArticles(page) }
            .unwrapEnvelope()
            .map { response ->
                ArticlePage(
                    articles = response.datas
                        .distinctBy(WanArticleDto::id)
                        .map(WanArticleDto::toModel),
                    hasMore = !response.over && response.curPage < response.pageCount,
                )
            }
}

private fun <T : Any> NetworkResult<WanAndroidEnvelope<T>>.unwrapEnvelope(): Result<T> = when (this) {
    is NetworkResult.Failure -> Result.failure(NetworkException(error))
    is NetworkResult.Success -> when {
        value.errorCode != 0 -> Result.failure(
            NetworkException(
                NetworkError.Api(
                    errorCode = value.errorCode,
                    errorMessage = value.errorMsg.ifBlank { null },
                ),
            ),
        )

        value.data == null -> Result.failure(
            NetworkException(
                NetworkError.InvalidPayload(
                    IllegalStateException("WanAndroid 成功响应缺少 data"),
                ),
            ),
        )

        else -> Result.success(value.data)
    }
}
