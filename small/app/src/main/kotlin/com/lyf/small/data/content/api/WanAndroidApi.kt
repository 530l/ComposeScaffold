package com.lyf.small.data.content.api

import com.lyf.small.data.content.dto.WanAndroidEnvelope
import com.lyf.small.data.content.dto.WanArticlePageDto
import com.lyf.small.data.content.dto.WanBannerDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

internal interface WanAndroidApi {
    @GET("banner/json")
    suspend fun getBanners(): Response<WanAndroidEnvelope<List<WanBannerDto>>>

    @GET("article/list/{page}/json")
    suspend fun getArticles(
        @Path("page") page: Int,
    ): Response<WanAndroidEnvelope<WanArticlePageDto>>
}
