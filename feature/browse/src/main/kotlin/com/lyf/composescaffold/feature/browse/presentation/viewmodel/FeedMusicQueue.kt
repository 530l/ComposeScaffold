package com.lyf.composescaffold.feature.browse.presentation.viewmodel

import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.music.MusicTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** 大厅对全局音乐播放器的播放视图：点击驱动全局播放器，过滤进度字段。 */
internal class FeedMusicQueue(private val controller: MusicPlayerController?) {
    // 过滤单纯的进度变化，减少大厅重组。
    val playbackState: Flow<MusicHallPlayback>? = controller?.state?.map {
        MusicHallPlayback(it.currentTrack?.id, it.isPlaying, it.wantsPlay, it.isBuffering, it.errorCode != null)
    }?.distinctUntilChanged()

    /** 大厅点击直接驱动全局音乐播放器，不经过页面内播放会话。 */
    fun play(track: MusicTrack, queue: List<MusicTrack>) {
        val player = controller ?: return
        if (player.state.value.currentTrack?.id == track.id) {
            // 当前曲目操作保留进度；错误重试沿用全局播放器的恢复入口。
            if (player.state.value.errorCode != null) player.resume() else player.toggle()
        } else {
            player.playTrack(track, queue)
        }
    }
}
