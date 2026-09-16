package com.lyf.small.data.content.repository

import com.lyf.small.core.common.log.AppLogger
import com.lyf.small.data.content.api.WanAndroidApi
import com.lyf.small.data.content.dto.WanAndroidError
import com.lyf.small.data.content.dto.WanArticleDto
import com.lyf.small.data.content.dto.WanBannerDto
import com.lyf.small.data.content.mapper.toModel
import com.lyf.small.data.content.model.ArticlePage
import com.lyf.small.data.content.model.Banner
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.envelope.unwrap
import com.skydoves.sandwich.mapSuccess
import com.skydoves.sandwich.onFailure
import retrofit2.Response
import javax.inject.Inject

/** 多个 Feature 可共同依赖的内容数据契约。 */
interface ContentRepository {
    suspend fun loadBanners(): ApiResponse<List<Banner>>

    suspend fun loadArticles(page: Int): ApiResponse<ArticlePage>
}

/** 信封拍平与 DTO 去重映射都在链上完成，失败统一由 ApiResponse 表达、永不抛出。 */
internal class DefaultContentRepository @Inject constructor(
    private val api: WanAndroidApi,
) : ContentRepository {
    override suspend fun loadBanners(): ApiResponse<List<Banner>> =
        api.getBanners()
            .unwrap()
            .mapSuccess {
                orEmpty().distinctBy(WanBannerDto::id).map(WanBannerDto::toModel)
            }
            .onFailure { AppLogger.warning(TAG) { "loadBanners: ${failureSummary()}" } }

    override suspend fun loadArticles(page: Int): ApiResponse<ArticlePage> =
        api.getArticles(page)
            .unwrap()
            .mapSuccess {
                val dto = this
                ArticlePage(
                    articles = dto?.datas.orEmpty()
                        .distinctBy(WanArticleDto::id)
                        .map(WanArticleDto::toModel),
                    hasMore = dto != null && !dto.over && dto.curPage < dto.pageCount,
                )
            }
            .onFailure { AppLogger.warning(TAG) { "loadArticles: ${failureSummary()}" } }

    private companion object {
        const val TAG = "ContentRepository"
    }
}

/** 信封业务错误码；仅 WanAndroid 业务失败时非空。 */
internal fun ApiResponse.Failure<*>.envelopeErrorCode(): Int? =
    ((this as? ApiResponse.Failure.Error)?.payload as? WanAndroidError)?.code

/** 日志脱敏摘要：业务错误只记错误码、HTTP 错误只记状态码、异常只记类名；响应正文与服务端文案永不进日志。 */
internal fun ApiResponse.Failure<*>.failureSummary(): String = when (this) {
    is ApiResponse.Failure.Error -> when (val errorPayload = payload) {
        is WanAndroidError -> "API errorCode=${errorPayload.code}"
        is Response<*> -> "HTTP ${errorPayload.code()}"
        else -> errorPayload?.javaClass?.simpleName ?: "unknown error"
    }
    is ApiResponse.Failure.Exception -> throwable::class.simpleName ?: "Throwable"
}
