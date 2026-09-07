package com.lyf.composescaffold.core.player.playback

import com.lyf.composescaffold.core.player.media.MediaSource
import kotlinx.coroutines.flow.StateFlow

/** 播放单个媒体；队列、切歌和书签由调用方管理。 */
interface Player {
    /** 本次借用期间固定的媒体描述。 */
    val source: MediaSource

    /** 播放状态流，供 UI 收集。 */
    val state: StateFlow<PlayerState>

    /** 按需读取的进度快照，与状态流分开更新。 */
    val position: PlaybackProgress

    /** 视频输出接口，音频为 null。 */
    val video: VideoOutput?

    /** 设置播放意图，实际状态和后台启动错误通过 state 返回。 */
    fun setPlaying(playing: Boolean)

    /** 跳转到指定毫秒位置；不支持 Seek 或时长未知时忽略。 */
    fun seekTo(positionMs: Long)

    /** 重新预备当前媒体以尝试恢复错误，结果通过 state 返回。 */
    fun retry()
}
