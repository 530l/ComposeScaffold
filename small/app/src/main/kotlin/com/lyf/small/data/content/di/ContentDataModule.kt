package com.lyf.small.data.content.di

import com.lyf.small.core.data.network.PublicRetrofit
import com.lyf.small.core.data.network.SessionEventManager
import com.lyf.small.data.content.api.AppApi
import com.lyf.small.data.content.network.AppLoginExpiredMapper
import com.lyf.small.data.content.repository.ContentRepository
import com.lyf.small.data.content.repository.DefaultContentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ContentDataModule {
    @Binds
    @Singleton
    abstract fun bindContentRepository(
        repository: DefaultContentRepository,
    ): ContentRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppApi(
            @PublicRetrofit retrofit: Retrofit,
        ): AppApi = retrofit.create(AppApi::class.java)

        @Provides
        @Singleton
        fun provideAppLoginExpiredMapper(
            sessionEventManager: SessionEventManager,
        ): AppLoginExpiredMapper = AppLoginExpiredMapper(sessionEventManager)
    }
}
