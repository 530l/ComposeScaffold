package com.lyf.composescaffold.feature.browse.playback.di

import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.feature.browse.playback.global.GlobalMusicPlayer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class MusicPlaybackScope

/** 音乐控制器使用进程作用域，页面退出不取消后台播放。 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MusicPlayerModule {

    /** 绑定单例音乐控制器实现 */
    @Binds
    @Singleton
    abstract fun bindMusicPlayerController(impl: GlobalMusicPlayer): MusicPlayerController

    companion object {
        /** 提供全局播放器专用进程级协程作用域。 */
        @Provides
        @Singleton
        @MusicPlaybackScope
        fun provideGlobalPlayerScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
