package com.lyf.composescaffold.feature.browse.presentation.mv.playback.di

import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackPolicy
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.ShortVideoPlaybackPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedPlaybackModule {
    /** 提供共享策略：普通设备最多两个播放器，只预备一个相邻项。 */
    @Provides
    @Singleton
    fun playbackPolicy(): FeedPlaybackPolicy = ShortVideoPlaybackPolicy()
}
