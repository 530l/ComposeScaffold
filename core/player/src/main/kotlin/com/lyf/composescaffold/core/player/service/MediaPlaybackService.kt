package com.lyf.composescaffold.core.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lyf.composescaffold.core.player.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.concurrent.CancellationException

/** 承载后台播放和系统媒体控制的前台服务。 */
@AndroidEntryPoint
internal class MediaPlaybackService : MediaSessionService() {
    /** 统一管理播放权和系统会话。 */
    @Inject
    lateinit var sessionManager: MediaSessionOwner

    /** 服务当前绑定的会话。 */
    private var attachedSession: MediaSession? = null

    /** 绑定当前后台播放会话。 */
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        attachedSession = sessionManager.attachService(this)?.also(::addSession)
    }

    /** 处理启动请求；没有后台播放任务时停止服务。 */
    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Media3 内部启动命令不再覆盖正式通知。
        if (intent?.action == ACTION_START) {
            val ticket = intent.getLongExtra(EXTRA_GENERATION, -1L)
            try {
                if (!isPlaybackOngoing) startForegroundImmediately()
            } catch (_: RuntimeException) {
                sessionManager.backgroundFailed(this, ticket)
                return START_NOT_STICKY
            }
            val current = sessionManager.sessionForService()
            if (current == null) {
                stopPlayback()
                return START_NOT_STICKY
            }
            if (attachedSession !== current) {
                attachedSession?.let(::removeSession)
                attachedSession = current.also(::addSession)
            }
            triggerNotificationUpdate()
        }
        if (sessionManager.sessionForService() == null) {
            stopPlayback()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /** 记录本次播放申请编号，忽略过期的通知失败结果。 */
    @OptIn(UnstableApi::class)
    override fun onUpdateNotificationAsync(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ): ListenableFuture<Void?> {
        val ticket = sessionManager.backgroundGeneration()
        val result = try {
            super.onUpdateNotificationAsync(session, startInForegroundRequired)
        } catch (error: RuntimeException) {
            Futures.immediateFailedFuture<Void?>(error)
        }
        Futures.addCallback(result, object : FutureCallback<Void?> {
            override fun onSuccess(result: Void?) = Unit
            override fun onFailure(error: Throwable) {
                if (error is CancellationException) return
                if (startInForegroundRequired && ticket != null) {
                    sessionManager.backgroundFailed(this@MediaPlaybackService, ticket)
                }
            }
        }, ContextCompat.getMainExecutor(this))
        return result
    }

    /** 仅向控制端提供开启后台播放的会话。 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        sessionManager.sessionForService()

    /** 解绑会话、移除前台通知并停止服务。 */
    internal fun stopPlayback() {
        attachedSession?.let(::removeSession)
        attachedSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 解绑会话并清除服务引用。 */
    override fun onDestroy() {
        attachedSession?.let(::removeSession)
        attachedSession = null
        sessionManager.detachService(this)
        super.onDestroy()
    }

    /** 先显示占位通知，满足前台服务的启动时限。 */
    private fun startForegroundImmediately() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    /** 低优先级通道的常驻占位通知。 */
    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    PLAYBACK_CHANNEL_ID,
                    getString(R.string.player_playback_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, PLAYBACK_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.player_playback_notification))
            .setOngoing(true)
            .build()
    }

    companion object {
        /** 本模块的启动动作，与 Media3 内部命令区分。 */
        internal const val ACTION_START = "com.lyf.composescaffold.core.player.START"
        internal const val EXTRA_GENERATION = "playback_generation"
        private const val PLAYBACK_CHANNEL_ID = "media_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
