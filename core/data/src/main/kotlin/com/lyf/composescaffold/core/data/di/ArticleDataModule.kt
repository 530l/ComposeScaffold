package com.lyf.composescaffold.core.data.di

import com.lyf.composescaffold.core.data.article.ArticleListApi
import com.lyf.composescaffold.core.data.article.ArticleRepository
import com.lyf.composescaffold.core.data.article.DefaultArticleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArticleDataModule {

    @Binds
    @Singleton
    internal abstract fun bindArticleRepository(impl: DefaultArticleRepository): ArticleRepository

    companion object {
        @Provides
        @Singleton
        fun provideArticleListApi(retrofit: Retrofit): ArticleListApi =
            retrofit.create(ArticleListApi::class.java)
    }
}
