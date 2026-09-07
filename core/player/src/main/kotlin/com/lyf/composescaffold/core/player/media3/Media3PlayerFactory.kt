package com.lyf.composescaffold.core.player.media3

import com.lyf.composescaffold.core.player.config.BufferConfig
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import com.lyf.composescaffold.core.player.playback.PlayerPool as CorePlayerPool
import com.lyf.composescaffold.core.player.service.MediaSessionOwner
import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlayerPool
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/** 共享缓存的播放器工厂，调用方应按进程单例使用。 */
@androidx.annotation.OptIn(UnstableApi::class)
class Media3PlayerFactory(
    /** 应用上下文。 */
    private val context: Context,
    /** 请求媒体数据的公共 HTTP 客户端。 */
    private val client: OkHttpClient,
    /** 缓存初始化使用的 IO 调度器。 */
    private val ioDispatcher: CoroutineDispatcher,
    /** 管理系统会话和前台服务。 */
    private val sessionManager: MediaSessionOwner,
    /** 磁盘缓存容量上限（字节）。 */
    private val cacheBytes: Long = 256L * 1024 * 1024,
) : PlayerFactory {
    init { require(cacheBytes > 0) }

    /** 防止缓存被并发初始化的锁。 */
    private val cacheMutex = Mutex()

    /** 本工厂创建的池共用此缓存。 */
    private var cache: SimpleCache? = null

    /** 是否为系统标记的低内存设备。 */
    override val lowRam: Boolean get() = context.getSystemService(ActivityManager::class.java).isLowRamDevice

    /** 等待缓存初始化后，按容量和缓冲配置创建池。 */
    override suspend fun createPool(capacity: Int, buffer: BufferConfig): CorePlayerPool {
        require(capacity > 0)
        // 在 IO 调度器上初始化缓存，后续建池直接复用。
        val sharedCache = withContext(ioDispatcher) {
            cacheMutex.withLock {
                cache ?: SimpleCache(
                    File(context.cacheDir, "feed-media"),
                    LeastRecentlyUsedCacheEvictor(cacheBytes),
                    StandaloneDatabaseProvider(context),
                ).also { cache = it }
            }
        }
        val dataSource = CacheDataSource.Factory()
            .setCache(sharedCache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, OkHttpDataSource.Factory(client)))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return Media3PlayerPool(PlayerPool(capacity) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(buffer.minBufferMs, buffer.maxBufferMs, buffer.startBufferMs, buffer.rebufferMs)
                        .setTargetBufferBytes(buffer.targetBytes)
                        .setPrioritizeTimeOverSizeThresholds(false)
                        .build(),
                )
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = false
                }
        }, sessionManager)
    }
}
