package com.lyf.composescaffold.feature.cart.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * wanandroid 文章列表，脚手架演示数据源（只接这一个接口）。
 * 页码 0 起：`article/list/0/json` 为第一页。
 */
internal interface ArticleListApi {
    @GET("article/list/{page}/json")
    suspend fun getArticleList(
        @Path("page") page: Int,
    ): retrofit2.Response<WanApiResponse<WanArticleListDto>>
}
