package com.lyf.composescaffold.core.data.article

import com.lyf.composescaffold.core.model.article.WanApiResponse
import com.lyf.composescaffold.core.model.article.WanArticleListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * wanandroid 文章列表接口，按业务领域收拢在 core:data/article。
 */
interface ArticleListApi {
    @GET("article/list/{page}/json")
    suspend fun getArticleList(
        @Path("page") page: Int,
    ): Response<WanApiResponse<WanArticleListResponse>>
}
