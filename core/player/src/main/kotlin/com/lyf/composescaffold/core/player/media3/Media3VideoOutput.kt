package com.lyf.composescaffold.core.player.media3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.ui.compose.PlayerSurface
import com.lyf.composescaffold.core.player.playback.VideoOutput
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 使用 PlayerSurface 显示视频，并跟踪 Surface 和首帧。
 * ForwardingPlayer 拦截画面绑定，其余调用转发给 ExoPlayer。
 */
@UnstableApi
internal class Media3VideoOutput(
    /** 负责实际解码和渲染的 ExoPlayer。 */
    private val exoPlayer: ExoPlayer,
    /** 当前媒体 ID，用于过滤其他媒体的事件。 */
    private val mediaId: String,
    /** 获取视频尺寸前使用的初始宽高比。 */
    initialAspectRatio: Float,
) : ForwardingPlayer(exoPlayer), VideoOutput {
    /** 初始显示封面，当前 Surface 收到首帧后撤下。 */
    private val _covered = MutableStateFlow(true)
    override val covered = _covered.asStateFlow()
    private val _aspectRatio = MutableStateFlow(initialAspectRatio)
    override val aspectRatio = _aspectRatio.asStateFlow()

    /** 当前绑定的 SurfaceView。 */
    private var surfaceView: SurfaceView? = null

    /** 标记输出已关闭，阻止后续绑定和播放事件处理。 */
    private var closed = false

    /** Surface 销毁或重建时显示封面。 */
    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) { _covered.value = true }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
        override fun surfaceDestroyed(holder: SurfaceHolder) { _covered.value = true }
    }

    /** 根据当前媒体的视频尺寸更新宽高比。 */
    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (!closed && exoPlayer.currentMediaItem?.mediaId == mediaId && videoSize.width > 0 && videoSize.height > 0) {
                _aspectRatio.value = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            }
        }
    }

    /** 确认媒体和 Surface 均匹配后，撤下首帧遮罩。 */
    private val analytics = object : AnalyticsListener {
        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
            if (!closed && exoPlayer.currentMediaItem?.mediaId == mediaId &&
                output === surfaceView?.holder?.surface
            ) {
                _covered.value = false
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.addAnalyticsListener(analytics)
    }

    /** 绑定新 Surface 时重置遮罩，再转发给 ExoPlayer。 */
    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        if (closed) return
        if (this.surfaceView !== surfaceView) {
            this.surfaceView?.holder?.removeCallback(surfaceCallback)
            _covered.value = true
            this.surfaceView = surfaceView
            surfaceView?.holder?.addCallback(surfaceCallback)
        }
        super.setVideoSurfaceView(surfaceView)
    }

    /** 移除当前 Surface 的监听，再转发解绑操作。 */
    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        if (closed || this.surfaceView !== surfaceView) return
        this.surfaceView?.holder?.removeCallback(surfaceCallback)
        this.surfaceView = null
        _covered.value = true
        super.clearVideoSurfaceView(surfaceView)
    }

    /** 直接用 Media3 的 PlayerSurface 渲染。 */
    @Composable
    override fun Render(modifier: Modifier) {
        PlayerSurface(this, modifier)
    }

    /** 归还播放器前，解绑 Surface 并移除监听。 */
    fun close() {
        if (closed) return
        surfaceView?.let { clearVideoSurfaceView(it) }
        closed = true
        exoPlayer.removeListener(playerListener)
        exoPlayer.removeAnalyticsListener(analytics)
    }
}
