package com.lyf.composescaffold.core.data.api

import com.lyf.composescaffold.core.model.article.WanApiResponse
import com.lyf.composescaffold.core.model.article.WanArticleListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 购物车相关 API 接口（按模块前缀命名，扁平存放在 core:data/api）。
 */
interface CartApi {
    @GET("article/list/{page}/json")
    suspend fun getArticleList(
        @Path("page") page: Int,
    ): Response<WanApiResponse<WanArticleListResponse>>
}
