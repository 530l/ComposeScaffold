package com.lyf.composescaffold.ui.player

import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerViewModelTest {
    private class Controller : MusicPlayerController {
        override val state = MutableStateFlow(MusicPlaybackSnapshot())
        override val queue = MutableStateFlow(MusicQueue())
        val seeks = mutableListOf<Long>()
        override fun seekTo(positionMs: Long) { seeks.add(positionMs) }
        override fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>?) = Unit
        override fun playAt(index: Int) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun toggle() = Unit
        override fun next() = Unit
        override fun previous() = Unit
        override fun setPlayMode(mode: PlayMode) = Unit
        override fun removeFromQueue(trackId: String) = Unit
        override fun clearQueue() = Unit
    }

    @Test
    fun skipControlsClampAtBothEnds() {
        val controller = Controller()
        val viewModel = MiniPlayerViewModel(controller)
        controller.state.value = MusicPlaybackSnapshot(positionMs = 5_000, durationMs = 10_000, seekable = true)
        viewModel.seekRewind()
        viewModel.seekForward()
        assertEquals(listOf(0L, 10_000L), controller.seeks)
    }

    @Test
    fun staleSeekAfterSourceLosesSeekabilityIsIgnored() {
        val controller = Controller()
        val viewModel = MiniPlayerViewModel(controller)
        controller.state.value = MusicPlaybackSnapshot(durationMs = 10_000, seekable = false)
        viewModel.seekTo(5_000)
        controller.state.value = MusicPlaybackSnapshot(durationMs = 0, seekable = true)
        viewModel.seekForward()
        assertTrue(controller.seeks.isEmpty())
    }
}
