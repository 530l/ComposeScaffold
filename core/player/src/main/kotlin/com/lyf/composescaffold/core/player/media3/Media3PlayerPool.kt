package com.lyf.composescaffold.core.player.media3

import com.lyf.composescaffold.core.player.service.MediaSessionOwner
import androidx.media3.common.PlayerPool
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.lyf.composescaffold.core.player.media.MediaSource
import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.playback.Player
import com.lyf.composescaffold.core.player.playback.PlayerPool as CorePlayerPool

/** 适配 Media3 对象池，管理播放器借用记录。 */
@UnstableApi
internal class Media3PlayerPool(
    /** 负责创建和复用 ExoPlayer 的底层池。 */
    private val pool: PlayerPool<ExoPlayer>,
    /** 管理系统会话和前台服务。 */
    private val sessionManager: MediaSessionOwner,
) : CorePlayerPool {
    /** 本池已借出的播放器。 */
    private val leases = mutableSetOf<Media3Player>()

    /** 借用底层播放器并预备媒体。 */
    override suspend fun acquire(source: MediaSource, positionMs: Long, options: PlaybackOptions): Player {
        val player = pool.acquire()
        var created: Media3Player? = null
        return try {
            Media3Player(source, options, player, sessionManager).also { acquired ->
                created = acquired
                acquired.prepare(positionMs)
                leases.add(acquired)
            }
        } catch (error: Exception) {
            created?.detach()
            pool.yield(player)
            throw error
        }
    }

    /** 清理监听后，将播放器归还底层池。 */
    override fun recycle(player: Player) {
        val lease = leases.firstOrNull { it === player } ?: return
        leases.remove(lease)
        lease.detach()
        pool.yield(lease.player)
    }

    /** 回收所有借用记录并释放底层池。 */
    override fun release() {
        leases.toList().forEach(::recycle)
        pool.release()
    }
}
