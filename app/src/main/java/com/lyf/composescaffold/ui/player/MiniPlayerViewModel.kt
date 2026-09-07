package com.lyf.composescaffold.ui.player

import androidx.lifecycle.ViewModel
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.PlayMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** 全局常驻迷你悬浮播放器与播单抽屉的状态持有者。 */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(
    private val controller: MusicPlayerController,
) : ViewModel() {

    val playbackState: StateFlow<MusicPlaybackSnapshot> = controller.state
    val queueState: StateFlow<MusicQueue> = controller.queue

    fun toggle() = controller.toggle()

    fun next() = controller.next()

    fun previous() = controller.previous()

    fun seekTo(positionMs: Long) {
        val snapshot = playbackState.value
        // 松手前可能已切歌或失去 Seek 能力，按最新状态校验。
        if (!snapshot.seekable || snapshot.durationMs <= 0) return
        controller.seekTo(positionMs.coerceIn(0, snapshot.durationMs))
    }

    fun seekForward(deltaMs: Long = 15_000L) {
        val current = playbackState.value.positionMs
        val duration = playbackState.value.durationMs
        seekTo((current + deltaMs).coerceAtMost(duration))
    }

    fun seekRewind(deltaMs: Long = 15_000L) {
        val current = playbackState.value.positionMs
        seekTo((current - deltaMs).coerceAtLeast(0L))
    }

    fun playAt(index: Int) = controller.playAt(index)

    fun setPlayMode(mode: PlayMode) = controller.setPlayMode(mode)

    fun cyclePlayMode() {
        val current = queueState.value.playMode
        val nextMode = when (current) {
            PlayMode.LIST_LOOP -> PlayMode.SINGLE_LOOP
            PlayMode.SINGLE_LOOP -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.LIST_LOOP
        }
        controller.setPlayMode(nextMode)
    }

    fun removeTrack(trackId: String) = controller.removeFromQueue(trackId)

    fun clearQueue() = controller.clearQueue()
}
