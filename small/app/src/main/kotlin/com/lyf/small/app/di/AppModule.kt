package com.lyf.small.app.di

import android.content.Context
import coil3.ImageLoader
import com.lyf.small.BuildConfig
import com.lyf.small.core.common.config.AppConfig
import com.lyf.small.core.common.config.AppEnvironment
import com.lyf.small.core.data.network.PublicHttpClient
import com.lyf.small.core.design.image.createAppImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** 应用组合根：运行环境来自 BuildConfig，基础组件通过 Hilt 按需创建。 */
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

    /** 图片加载器只使用无认证客户端，避免向公共图片域名发送业务凭据。 */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @PublicHttpClient okHttpClient: OkHttpClient,
    ): ImageLoader = createAppImageLoader(context, okHttpClient)
}
