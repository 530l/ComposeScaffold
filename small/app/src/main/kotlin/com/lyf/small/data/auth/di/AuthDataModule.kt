package com.lyf.small.data.auth.di

import com.lyf.small.core.infra.network.SessionEventManager
import com.lyf.small.data.auth.AppLoginExpiredMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthDataModule {

    @Provides
    @Singleton
    fun provideAppLoginExpiredMapper(
        sessionEventManager: SessionEventManager,
    ): AppLoginExpiredMapper = AppLoginExpiredMapper(sessionEventManager)
}
