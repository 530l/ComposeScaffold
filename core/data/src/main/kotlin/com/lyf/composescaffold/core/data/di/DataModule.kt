package com.lyf.composescaffold.core.data.di

import com.lyf.composescaffold.core.data.network.createJson
import com.lyf.composescaffold.core.data.network.createOkHttpClient
import com.lyf.composescaffold.core.data.network.createRetrofit
import com.lyf.composescaffold.core.data.storage.KeyValueStore
import com.lyf.composescaffold.core.data.storage.MmkvKeyValueStore
import com.lyf.composescaffold.core.common.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * core:data 基础设施：AppConfig 由 :app 的 AppModule 从 BuildConfig 提供。
 * Hilt 惰性构造：首次注入才创建 MmkvKeyValueStore，但 MMKV.initialize
 * 早在 Application.onCreate 完成，构造时序天然安全。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideJson(): Json = createJson()

    @Provides
    @Singleton
    fun provideOkHttpClient(config: AppConfig): OkHttpClient = createOkHttpClient(config)

    @Provides
    @Singleton
    fun provideRetrofit(
        config: AppConfig,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = createRetrofit(config, okHttpClient, json)

    @Provides
    @Singleton
    fun provideKeyValueStore(): KeyValueStore = MmkvKeyValueStore()
}
