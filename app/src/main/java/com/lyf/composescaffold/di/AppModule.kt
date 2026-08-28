package com.lyf.composescaffold.di

import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import com.lyf.composescaffold.BuildConfig
import com.lyf.composescaffold.core.common.config.AppConfig
import com.lyf.composescaffold.core.common.config.AppEnvironment
import com.lyf.composescaffold.core.design.image.createAppImageLoader
import com.lyf.composescaffold.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** 组合根模块：环境配置从 BuildConfig 注入（debug/release 各自 baseUrl）。 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        environment = if (BuildConfig.DEBUG) {
            AppEnvironment.DEVELOPMENT
        } else {
            AppEnvironment.PRODUCTION
        },
        apiBaseUrl = BuildConfig.API_BASE_URL,
        enableNetworkLogging = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "compose_scaffold.db",
        ).build()

    /** 图片加载器进程单例：复用网络栈共享的 OkHttpClient，OkHttpClient 不越过此层暴露给 UI。 */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader = createAppImageLoader(context, okHttpClient)
}
