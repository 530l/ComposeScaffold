package com.lyf.composescaffold.core.player.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.lyf.composescaffold.core.common.log.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 在主线程交接播放权；仅为后台播放启动前台服务。 */
@OptIn(UnstableApi::class)
@Singleton
class MediaSessionOwner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** 按需创建的共享系统媒体会话。 */
    private var session: MediaSession? = null

    /** 当前持有播放权的播放器。 */
    private var owner: Player? = null

    /** 会话使用的占位播放器，不创建解码器。 */
    private var stub: Player? = null

    /** 当前关联的服务实例。 */
    private var service: MediaPlaybackService? = null

    /** 当前播放器是否开启后台播放。 */
    private var backgroundOwner = false

    /** 播放申请编号，用于识别过期回调。 */
    private var generation = 0L

    /** 通知当前播放器后台启动失败。 */
    private var onBackgroundFailure: (() -> Unit)? = null

    /** 按需创建会话，先绑定不占解码器的占位播放器。 */
    private fun getOrCreateSession(): MediaSession {
        session?.let { return it }
        val placeholder = object : SimpleBasePlayer(Looper.getMainLooper()) {
            override fun getState(): State = State.Builder().build()
        }.also { stub = it }
        return MediaSession.Builder(context, placeholder).setId("MediaPlaybackSession").build()
            .also { session = it }
    }

    /** 先暂停旧播放器，再交接会话；后台服务启动失败返回 false。 */
    internal fun activate(player: Player, background: Boolean, onFailure: () -> Unit): Boolean {
        val ticket = ++generation
        if (!background) stopService()
        if (owner !== player) {
            owner?.pause()
            getOrCreateSession().setPlayer(player)
            owner = player
        }
        backgroundOwner = background
        onBackgroundFailure = if (background) onFailure else null
        if (!background) return true
        return try {
            val intent = Intent(context, MediaPlaybackService::class.java).setAction(MediaPlaybackService.ACTION_START)
                .putExtra(MediaPlaybackService.EXTRA_GENERATION, ticket)
            val component = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            checkNotNull(component)
            true
        } catch (error: Exception) {
            AppLogger.warning("MediaPlayback") { "启动媒体服务失败 type=${error.javaClass.simpleName}" }
            backgroundFailed(ticket)
            false
        }
    }

    /** 记录服务实例，并返回可绑定的后台会话。 */
    internal fun attachService(service: MediaPlaybackService): MediaSession? {
        this.service = service
        return sessionForService()
    }

    /** 仅返回开启后台播放的会话。 */
    internal fun sessionForService(): MediaSession? = session.takeIf { backgroundOwner }

    /** 服务销毁时回调。 */
    internal fun detachService(service: MediaPlaybackService) {
        if (this.service !== service) return
        this.service = null
        releaseIfIdle()
    }

    internal fun backgroundGeneration(): Long? = generation.takeIf { backgroundOwner }

    /** 服务实例和申请代次必须同时匹配。 */
    internal fun backgroundFailed(service: MediaPlaybackService, ticket: Long) {
        if (this.service !== service) return
        backgroundFailed(ticket)
    }

    private fun backgroundFailed(ticket: Long) {
        if (!backgroundOwner || ticket != generation) return
        val callback = onBackgroundFailure
        owner?.pause()
        stopService()
        backgroundOwner = false
        onBackgroundFailure = null
        callback?.invoke()
    }

    /** 归还当前播放器时，停止服务并释放会话。 */
    internal fun detach(player: Player) {
        if (owner !== player) return
        generation++
        stopService()
        owner = null
        backgroundOwner = false
        onBackgroundFailure = null
        stub?.let { session?.setPlayer(it) }
        releaseIfIdle()
    }

    /** 停止服务并移除前台通知。 */
    private fun stopService() {
        service?.stopPlayback()
        if (backgroundOwner) context.stopService(Intent(context, MediaPlaybackService::class.java))
    }

    /** 没有播放器持有播放权时，释放会话和占位播放器。 */
    private fun releaseIfIdle() {
        if (owner != null) return
        session?.release()
        session = null
        stub?.release()
        stub = null
    }
}
