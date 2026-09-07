package com.lyf.composescaffold.core.data.music

import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode
import kotlinx.coroutines.flow.StateFlow

/** 音乐大厅与悬浮窗共用的控制入口，不暴露 Media3。 */
interface MusicPlayerController {
    val state: StateFlow<MusicPlaybackSnapshot>

    val queue: StateFlow<MusicQueue>

    fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>? = null)

    fun playAt(index: Int)

    fun pause()

    fun resume()

    fun toggle()

    fun seekTo(positionMs: Long)

    fun next()

    fun previous()

    fun setPlayMode(mode: PlayMode)

    fun removeFromQueue(trackId: String)

    fun clearQueue()
}
