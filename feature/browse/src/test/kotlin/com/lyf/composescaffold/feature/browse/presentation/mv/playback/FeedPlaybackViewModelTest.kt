package com.lyf.composescaffold.feature.browse.presentation.mv.playback

import com.lyf.composescaffold.core.player.config.BufferConfig
import com.lyf.composescaffold.core.player.playback.PlayerPool
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test

/** FeedPlaybackViewModel 的模式会话隔离与书签保存恢复测试。 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedPlaybackViewModelTest {
    /** 进入音乐大厅不复用 MV 选择，也不创建 MV 池。 */
    @Test
    fun enteringMusicHallDoesNotReusePreviousMvSelection() {
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val factory = object : PlayerFactory {
                override val lowRam = false
                override suspend fun createPool(capacity: Int, buffer: BufferConfig): PlayerPool = error("音乐大厅不应创建 MV 池")
            }
            val owner = FeedPlaybackViewModel(factory, ShortVideoPlaybackPolicy(), Controller(), SavedStateHandle())
            val mixed = owner.bind(FeedMode.MIXED)
            mixed.select(FeedItem("mv", "MV", "Artist", null, FeedMedia.Mv("https://example.com/mv.mp4")))
            val music = owner.bind(FeedMode.MUSIC)
            assertThat(mixed.state.value.key).isNull()
            assertThat(music.state.value.key).isNull()
            assertThat(owner.bind(FeedMode.MUSIC)).isSameInstanceAs(music)
            music.close()
        } finally { Dispatchers.resetMain() }
    }

    /** 书签由播放 ViewModel 保存，重建后可按模式恢复。 */
    @Test
    fun bookmark_isSavedByPlaybackViewModelAndRestored() {
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val savedState = SavedStateHandle()
            val factory = object : PlayerFactory {
                override val lowRam = false
                override suspend fun createPool(capacity: Int, buffer: BufferConfig): PlayerPool = error("不应创建播放器")
            }
            val controller = Controller()
            val item = FeedItem("item_3", "Music", "Artist", null, FeedMedia.Music("https://example.com/a.mp3"))
            val owner = FeedPlaybackViewModel(factory, ShortVideoPlaybackPolicy(), controller, savedState)
            owner.bind(FeedMode.MIXED) { listOf(item.copy(key = "0"), item.copy(key = "1"), item.copy(key = "2"), item) }
            owner.select(item)
            controller.state.value = MusicPlaybackSnapshot(
                currentTrack = MusicTrack(item.key, item.title, item.artist, null, item.media.url),
                positionMs = 5_000, wantsPlay = true,
            )
            owner.close()
            val restored = FeedPlaybackViewModel(factory, ShortVideoPlaybackPolicy(), controller, savedState)
                .bookmark(FeedMode.MIXED)
            assertThat(restored).isEqualTo(FeedBookmark(3, "item_3", 5_000, true))
        } finally { Dispatchers.resetMain() }
    }

    /** 只暴露可控状态、不执行任何操作的音乐控制器替身。 */
    private class Controller : MusicPlayerController {
        override val state = MutableStateFlow(MusicPlaybackSnapshot())
        override val queue = MutableStateFlow(MusicQueue())
        override fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>?) = Unit
        override fun playAt(index: Int) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun toggle() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun next() = Unit
        override fun previous() = Unit
        override fun setPlayMode(mode: PlayMode) = Unit
        override fun removeFromQueue(trackId: String) = Unit
        override fun clearQueue() = Unit
    }
}
