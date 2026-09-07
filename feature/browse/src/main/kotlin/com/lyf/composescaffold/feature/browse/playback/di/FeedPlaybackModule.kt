package com.lyf.composescaffold.feature.browse.playback.di

import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackPolicy
import com.lyf.composescaffold.feature.browse.playback.ShortVideoPlaybackPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 浏览模块播放器与策略依赖注入模块（PlaybackModule）。 */
@Module
@InstallIn(SingletonComponent::class)
internal object FeedPlaybackModule {
    /** 提供短视频播放调度策略单例（2 个池化播放器，预加载前后 1 项，低内存容量）。 */
    @Provides
    @Singleton
    fun playbackPolicy(): FeedPlaybackPolicy = ShortVideoPlaybackPolicy()
}
