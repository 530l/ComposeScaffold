package com.lyf.composescaffold.feature.cart.data.remote

import com.lyf.composescaffold.feature.cart.data.WanApiResponse
import com.lyf.composescaffold.feature.cart.data.WanArticleListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * wanandroid 文章列表，脚手架演示数据源。
 * 页码 0 起：`article/list/0/json` 为第一页。
 */
internal interface ArticleListApi {
    @GET("article/list/{page}/json")
    suspend fun getArticleList(
        @Path("page") page: Int,
    ): Response<WanApiResponse<WanArticleListResponse>>
}
