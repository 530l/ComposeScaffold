package com.lyf.composescaffold.di

import android.content.Context
import com.lyf.composescaffold.core.data.coroutine.IoDispatcher
import com.lyf.composescaffold.core.data.network.PublicHttpClient
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import com.lyf.composescaffold.core.player.media3.Media3PlayerFactory
import com.lyf.composescaffold.core.player.service.MediaSessionOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient

/** 组合层决定网络与缓存依赖，播放器模块不依赖业务数据层。 */
@Module
@InstallIn(SingletonComponent::class)
internal object PlayerModule {
    @Provides
    @Singleton
    fun playbackFactory(
        @ApplicationContext context: Context,
        @PublicHttpClient client: OkHttpClient,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        sessionOwner: MediaSessionOwner,
    ): PlayerFactory = Media3PlayerFactory(context, client, ioDispatcher, sessionOwner)
}
