package com.lyf.composescaffold.feature.cart.di

import com.lyf.composescaffold.feature.cart.data.ArticleRepository
import com.lyf.composescaffold.feature.cart.data.DefaultArticleRepository
import com.lyf.composescaffold.feature.cart.data.remote.ArticleListApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CartModule {

    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: DefaultArticleRepository): ArticleRepository

    companion object {
        @Provides
        @Singleton
        fun provideArticleListApi(retrofit: Retrofit): ArticleListApi =
            retrofit.create(ArticleListApi::class.java)
    }
}
