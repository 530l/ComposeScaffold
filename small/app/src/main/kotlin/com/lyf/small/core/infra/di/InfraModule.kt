package com.lyf.small.core.infra.di

import android.content.Context
import com.lyf.small.core.common.config.AppConfig
import com.lyf.small.core.infra.network.ApiHttpClient
import com.lyf.small.core.infra.network.PublicHttpClient
import com.lyf.small.core.infra.network.PublicRetrofit
import com.lyf.small.core.infra.network.AuthInterceptor
import com.lyf.small.core.infra.network.SessionEventManager
import com.lyf.small.core.infra.network.createJson
import com.lyf.small.core.infra.network.createOkHttpClient
import com.lyf.small.core.infra.network.createRetrofit
import com.lyf.small.core.infra.storage.AndroidKeyStoreCredentialStore
import com.lyf.small.core.infra.storage.KeyValueStore
import com.lyf.small.core.infra.storage.MmkvKeyValueStore
import com.lyf.small.core.infra.storage.SecureCredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * 基础设施：AppConfig 由 app 组合根从 BuildConfig 提供。
 * Hilt 惰性构造：首次注入 KeyValueStore 时才初始化 MMKV，未使用时不增加启动成本。
 */
@Module
@InstallIn(SingletonComponent::class)
object InfraModule {

    @Provides
    @Singleton
    fun provideJson(): Json = createJson()

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        config: AppConfig,
        sessionEventManager: SessionEventManager,
    ): AuthInterceptor = AuthInterceptor(
        apiBaseUrl = config.apiBaseUrl.toHttpUrl(),
        tokenProvider = sessionEventManager::getAuthToken,
        sessionEventManager = sessionEventManager,
    )

    @Provides
    @Singleton
    @PublicHttpClient
    fun providePublicOkHttpClient(config: AppConfig): OkHttpClient = createOkHttpClient(config)

    @Provides
    @Singleton
    @ApiHttpClient
    fun provideApiOkHttpClient(
        @PublicHttpClient publicClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient = publicClient.newBuilder().addInterceptor(authInterceptor).build()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: AppConfig,
        @ApiHttpClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = createRetrofit(config, okHttpClient, json)

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(
        config: AppConfig,
        @PublicHttpClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = createRetrofit(config, okHttpClient, json)

    @Provides
    @Singleton
    fun provideKeyValueStore(@ApplicationContext context: Context): KeyValueStore =
        MmkvKeyValueStore(context)

    @Provides
    @Singleton
    fun provideSecureCredentialStore(@ApplicationContext context: Context): SecureCredentialStore =
        AndroidKeyStoreCredentialStore(context)
}
