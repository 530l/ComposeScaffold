package com.lyf.small.data.content.api

import com.lyf.small.data.content.dto.AppEnvelope
import com.lyf.small.data.content.dto.ArticlePageDto
import com.lyf.small.data.content.dto.BannerDto
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

internal interface AppApi {
    @GET("banner/json")
    suspend fun getBanners(): ApiResponse<AppEnvelope<List<BannerDto>>>

    @GET("article/list/{page}/json")
    suspend fun getArticles(
        @Path("page") page: Int,
    ): ApiResponse<AppEnvelope<ArticlePageDto>>
}
