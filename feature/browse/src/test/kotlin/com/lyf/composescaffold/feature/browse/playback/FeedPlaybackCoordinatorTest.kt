package com.lyf.composescaffold.feature.browse.playback

import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.media.MediaSource
import com.lyf.composescaffold.core.player.config.BufferConfig
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
import com.lyf.composescaffold.core.player.playback.PlayerState
import com.lyf.composescaffold.core.player.playback.PlayerPool
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import com.lyf.composescaffold.core.player.playback.Player
import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.music.MusicPlaybackStatus
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedPlaybackCoordinatorTest {

    private class FakePlayer(
        override val source: MediaSource,
        initialPosition: Long = 0,
    ) : Player {
        private val _state = MutableStateFlow(PlayerState(isPlaying = false))
        override val state = _state.asStateFlow()
        override var position = PlaybackProgress(positionMs = initialPosition, durationMs = 60_000, seekable = true)
        override val video: VideoOutput? = null

        fun emit(status: PlayerState) { _state.value = status }

        val playingCalls = mutableListOf<Boolean>()
        val seekCalls = mutableListOf<Long>()

        override fun setPlaying(playing: Boolean) {
            playingCalls.add(playing)
            _state.value = _state.value.copy(isPlaying = playing, wantsPlay = playing)
        }

        override fun seekTo(positionMs: Long) {
            seekCalls.add(positionMs)
            position = position.copy(positionMs = positionMs)
        }

        override fun retry() {
            _state.value = _state.value.copy(errorCode = null)
        }
    }

    private class FakePlayerPool : PlayerPool {
        val acquired = mutableListOf<FakePlayer>()
        val recycled = mutableListOf<Player>()
        var released = false

        override suspend fun acquire(source: MediaSource, positionMs: Long, options: PlaybackOptions): Player {
            return FakePlayer(source, positionMs).also { acquired.add(it) }
        }

        override fun recycle(player: Player) {
            recycled.add(player)
        }

        override fun release() {
            released = true
        }
    }

    private class FakePlayerFactory(
        override val lowRam: Boolean = false,
        val pool: FakePlayerPool = FakePlayerPool(),
    ) : PlayerFactory {
        var createPoolCalls = 0
        override suspend fun createPool(capacity: Int, buffer: BufferConfig): PlayerPool {
            createPoolCalls++
            return pool
        }
    }

    /** 全局播放器替身：只记录调用，不产生音频，state 可由测试驱动。 */
    private class FakeMusicPlayerController : MusicPlayerController {
        val playCalls = mutableListOf<Pair<MusicTrack, List<MusicTrack>>>()
        val pauseCalls = mutableListOf<Int>()
        val seekCalls = mutableListOf<Long>()
        val toggleCalls = mutableListOf<Int>()
        private val _state = MutableStateFlow(MusicPlaybackSnapshot())
        override val state = _state.asStateFlow()
        private val _queue = MutableStateFlow(MusicQueue())
        override val queue = _queue.asStateFlow()

        fun emit(snapshot: MusicPlaybackSnapshot) { _state.value = snapshot }

        override fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>?) {
            _state.value = MusicPlaybackSnapshot(currentTrack = track, wantsPlay = true, intentVersion = _state.value.intentVersion + 1)
            playCalls.add(track to (newQueue ?: emptyList()))
        }

        override fun playAt(index: Int) = Unit
        override fun pause() {
            pauseCalls.add(pauseCalls.size)
            _state.value = _state.value.copy(wantsPlay = false, intentVersion = _state.value.intentVersion + 1)
        }
        override fun resume() {
            _state.value = _state.value.copy(wantsPlay = true, intentVersion = _state.value.intentVersion + 1)
        }
        override fun toggle() { toggleCalls.add(toggleCalls.size) }
        override fun seekTo(positionMs: Long) { seekCalls.add(positionMs) }
        override fun next() = Unit
        override fun previous() = Unit
        override fun setPlayMode(mode: PlayMode) = Unit
        override fun removeFromQueue(trackId: String) = Unit
        override fun clearQueue() = Unit
    }

    private fun testFeedItem(key: String, isMv: Boolean = false): FeedItem = FeedItem(
        key = key,
        title = "Title $key",
        artist = "Artist",
        coverUrl = "https://example.com/cover.jpg",
        media = if (isMv) FeedMedia.Mv(url = "https://example.com/video.mp4")
                else FeedMedia.Music(url = "https://example.com/music.mp3"),
    )

    @Test
    fun scrollingAwayFromMusic_pausesAndCancellationResumes() = runTest {
        val controller = FakeMusicPlayerController()
        val session = FeedPlaybackCoordinator(FakePlayerFactory(), ShortVideoPlaybackPolicy(), this, {}, musicController = controller)
        try {
            session.select(testFeedItem("a"))
            runCurrent()
            session.setPrimarilyVisible(false)
            runCurrent()
            assertThat(controller.state.value.wantsPlay).isFalse()
            assertThat(session.state.value.wantsPlay).isTrue()
            session.setPrimarilyVisible(true)
            runCurrent()
            assertThat(controller.state.value.wantsPlay).isTrue()
        } finally { session.close() }
    }

    @Test
    fun musicToMvAndBack_resumesOnlyTheInterruptedTrack() = runTest {
        val controller = FakeMusicPlayerController()
        val session = FeedPlaybackCoordinator(FakePlayerFactory(), ShortVideoPlaybackPolicy(), this, {}, musicController = controller)
        try {
            session.select(testFeedItem("a"))
            runCurrent()
            session.select(testFeedItem("mv", isMv = true))
            assertThat(controller.state.value.wantsPlay).isFalse()
            session.select(testFeedItem("a"))
            runCurrent()
            assertThat(controller.state.value.wantsPlay).isTrue()
            assertThat(controller.playCalls).hasSize(1)
        } finally { session.close() }
    }

    @Test
    fun userPauseDuringInterruption_preventsAutomaticResume() = runTest {
        val controller = FakeMusicPlayerController()
        val session = FeedPlaybackCoordinator(FakePlayerFactory(), ShortVideoPlaybackPolicy(), this, {}, musicController = controller)
        try {
            session.select(testFeedItem("a"))
            runCurrent()
            session.setPrimarilyVisible(false)
            controller.pause()
            session.setPrimarilyVisible(true)
            runCurrent()
            assertThat(controller.state.value.wantsPlay).isFalse()
        } finally { session.close() }
    }

    @Test
    fun selectMv_andHostVisible_startsPlayback() = runTest {
        val pool = FakePlayerPool()
        val factory = FakePlayerFactory(pool = pool)
        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = {},
        )
        try {
            val item = testFeedItem("item_1", isMv = true)
            coordinator.select(item)
            coordinator.setHostVisible(true)
            runCurrent()

            assertThat(factory.createPoolCalls).isEqualTo(1)
            assertThat(pool.acquired).hasSize(1)
            assertThat(pool.acquired.first().playingCalls.lastOrNull()).isTrue()
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun delayedTeardown_forMv_preservesPlayerWithinGracePeriod_andReleasesAfterTimeout() = runTest {
        val pool = FakePlayerPool()
        val factory = FakePlayerFactory(pool = pool)
        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = {},
            teardownDelayMs = 1_000L,
        )
        try {
            // 使用 MV（短视频）测试退后台暂停与延时释放
            val item = testFeedItem("item_mv_1", isMv = true)
            coordinator.select(item)
            coordinator.setHostVisible(true)
            runCurrent()

            val player = pool.acquired.first()
            assertThat(player.playingCalls.lastOrNull()).isTrue()

            // 步骤 1：离开前台，MV 立即暂停但暂不释放 pool
            coordinator.setHostVisible(false)
            assertThat(player.playingCalls.lastOrNull()).isFalse()
            assertThat(pool.released).isFalse()

            // 步骤 2：在延时超时前返回前台（500ms < 1000ms），无缝恢复播放
            advanceTimeBy(500)
            coordinator.setHostVisible(true)
            runCurrent()
            assertThat(pool.released).isFalse()
            assertThat(player.playingCalls.lastOrNull()).isTrue()

            // 步骤 3：再次退后台并超时，彻底释放底层池
            coordinator.setHostVisible(false)
            advanceTimeBy(1_001)
            runCurrent()
            assertThat(pool.released).isTrue()
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun musicItem_delegatesToGlobalPlayer_withoutTouchingPlayerPool() = runTest {
        val pool = FakePlayerPool()
        val factory = FakePlayerFactory(pool = pool)
        val controller = FakeMusicPlayerController()
        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = {},
            musicController = controller,
        )
        try {
            val queueTracks = listOf(
                MusicTrack("item_1", "Title item_1", "Artist", null, "https://example.com/music.mp3"),
                MusicTrack("item_2", "Title item_2", "Artist", null, "https://example.com/music.mp3"),
            )
            coordinator.musicQueueProvider = { queueTracks }
            coordinator.select(testFeedItem("item_1", isMv = false))
            runCurrent()

            // 音乐交给全局播放器并携带当前 Feed 音乐队列，不创建任何页面级播放器。
            assertThat(controller.playCalls).hasSize(1)
            assertThat(controller.playCalls.first().first.id).isEqualTo("item_1")
            assertThat(controller.playCalls.first().second).isEqualTo(queueTracks)
            assertThat(factory.createPoolCalls).isEqualTo(0)
            assertThat(pool.acquired).isEmpty()

            // 会话状态镜像全局播放器，供黑胶与歌词展示。
            assertThat(coordinator.state.value.key).isEqualTo("item_1")
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun mvPlayback_pausesGlobalMusicPlayer() = runTest {
        val pool = FakePlayerPool()
        val factory = FakePlayerFactory(pool = pool)
        val controller = FakeMusicPlayerController()
        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = {},
            musicController = controller,
        )
        try {
            // 先让全局音乐处于播放状态，MV 起播才需要它让位；否则 interruptMusic 无曲目可暂停。
            controller.playTrack(MusicTrack("music_1", "Title music_1", "Artist", null, "https://example.com/music.mp3"), null)
            coordinator.select(testFeedItem("item_mv_1", isMv = true))
            coordinator.setHostVisible(true)
            runCurrent()

            // MV 起播前必须让全局音乐让位，避免双声道并行。
            assertThat(controller.pauseCalls).isNotEmpty()
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun checkpoint_recordsPositionAndPlayState() = runTest {
        val pool = FakePlayerPool()
        val factory = FakePlayerFactory(pool = pool)
        var lastCheckpoint: PlaybackCheckpoint? = null

        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = { lastCheckpoint = it },
        )
        try {
            val item1 = testFeedItem("item_1", isMv = true)
            val item2 = testFeedItem("item_2", isMv = true)

            coordinator.select(item1, positionMs = 1500L, playWhenReady = true)
            coordinator.setHostVisible(true)
            runCurrent()

            // 切换到第二首，必须把第一首检查点保存下来
            coordinator.select(item2, positionMs = 0L, playWhenReady = true)
            runCurrent()

            assertThat(lastCheckpoint).isNotNull()
            assertThat(lastCheckpoint?.key).isEqualTo("item_1")
            assertThat(lastCheckpoint?.positionMs).isEqualTo(1500L)
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun onCheckpoint_reassignment_honoredByCachedSession() = runTest {
        val factory = FakePlayerFactory()
        val firstHandler: (PlaybackCheckpoint) -> Unit = { error("首次注册的回调不应在刷新后继续使用") }
        var latestHandlerKey: String? = null

        val coordinator = FeedPlaybackCoordinator(
            factory = factory,
            policy = ShortVideoPlaybackPolicy(),
            scope = this,
            onCheckpoint = firstHandler,
        )
        try {
            // 宿主刷新回调（对应 obtainController 对缓存会话的重注册）后，检查点必须走新回调。
            coordinator.onCheckpoint = { checkpoint -> latestHandlerKey = checkpoint.key }
            coordinator.select(testFeedItem("item_1", isMv = true), positionMs = 100L)
            coordinator.select(testFeedItem("item_2", isMv = true), positionMs = 0L)

            assertThat(latestHandlerKey).isEqualTo("item_1")
        } finally {
            coordinator.close()
        }
    }
    @Test
    fun readyBeforeBufferDelay_doesNotShowStaleSpinner() = runTest {
        val factory = FakePlayerFactory()
        val session = FeedPlaybackCoordinator(factory, ShortVideoPlaybackPolicy(), this, {})
        try {
            session.select(testFeedItem("mv", true))
            session.setHostVisible(true)
            runCurrent()
            val player = factory.pool.acquired.single()
            player.emit(PlayerState(isBuffering = true, wantsPlay = true))
            runCurrent()
            advanceTimeBy(100)
            player.emit(PlayerState(wantsPlay = true))
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertThat(session.state.value.buffering).isFalse()
        } finally { session.close() }
    }

    @Test
    fun externalMusicChange_usesActualIdentityAndDoesNotSeekNewTrack() = runTest {
        val controller = FakeMusicPlayerController()
        val checkpoints = mutableListOf<PlaybackCheckpoint>()
        val session = FeedPlaybackCoordinator(FakePlayerFactory(), ShortVideoPlaybackPolicy(), this,
            checkpoints::add, musicController = controller)
        try {
            session.select(testFeedItem("a"), 5_000)
            runCurrent()
            controller.emit(MusicPlaybackSnapshot(
                currentTrack = MusicTrack("b", "B", "Artist", null, "https://example.com/b.mp3"),
                status = MusicPlaybackStatus.PAUSED, positionMs = 9_000, durationMs = 60_000,
                seekable = true, wantsPlay = false,
            ))
            runCurrent()
            assertThat(session.state.value.key).isEqualTo("b")
            assertThat(session.progress.value.positionMs).isEqualTo(9_000)
            assertThat(controller.seekCalls).isEmpty()
            session.close()
            assertThat(checkpoints.last()).isEqualTo(PlaybackCheckpoint("b", 9_000, false))
        } finally { session.close() }
    }

    @Test
    fun selectingAlreadyPlayingMusic_keepsProgressAndPauseIntent() = runTest {
        val controller = FakeMusicPlayerController()
        controller.emit(MusicPlaybackSnapshot(
            currentTrack = MusicTrack("a", "A", "Artist", null, "https://example.com/music.mp3"),
            status = MusicPlaybackStatus.PAUSED, positionMs = 8_500, wantsPlay = false,
        ))
        val session = FeedPlaybackCoordinator(FakePlayerFactory(), ShortVideoPlaybackPolicy(), this, {},
            musicController = controller)
        try {
            session.select(testFeedItem("a"))
            runCurrent()
            assertThat(controller.playCalls).isEmpty()
            assertThat(session.progress.value.positionMs).isEqualTo(8_500)
            assertThat(session.state.value.wantsPlay).isFalse()
        } finally { session.close() }
    }

    @Test
    fun mvDoesNotPreloadAnUnusedMusicPlayer() = runTest {
        val factory = FakePlayerFactory()
        val session = FeedPlaybackCoordinator(factory, ShortVideoPlaybackPolicy(), this, {})
        try {
            session.select(testFeedItem("mv", true))
            session.setHostVisible(true)
            session.preload(testFeedItem("music"))
            runCurrent()
            assertThat(factory.pool.acquired.map { it.source.id }).containsExactly("mv")
        } finally { session.close() }
    }

    @Test
    fun reselectingRetainedMv_seeksBackToStart() = runTest {
        val factory = FakePlayerFactory()
        val session = FeedPlaybackCoordinator(factory, ShortVideoPlaybackPolicy(), this, {})
        try {
            val first = testFeedItem("a", true)
            val second = testFeedItem("b", true)
            session.select(first, 20_000)
            session.setHostVisible(true)
            session.preload(second)
            runCurrent()
            val firstPlayer = factory.pool.acquired.first()
            session.select(second)
            session.preload(first)
            runCurrent()
            session.select(first)
            runCurrent()
            assertThat(firstPlayer.seekCalls).contains(0L)
        } finally { session.close() }
    }

    @Test
    fun externalPauseAndResume_updateIntentWithoutBackgroundMvPlayback() = runTest {
        val factory = FakePlayerFactory()
        val session = FeedPlaybackCoordinator(factory, ShortVideoPlaybackPolicy(), this, {})
        try {
            session.select(testFeedItem("mv", true))
            session.setHostVisible(true)
            runCurrent()
            val player = factory.pool.acquired.single()
            player.emit(PlayerState(wantsPlay = false, externalIntentVersion = 1))
            runCurrent()
            assertThat(session.state.value.wantsPlay).isFalse()
            player.emit(PlayerState(wantsPlay = true, isPlaying = true, externalIntentVersion = 2))
            runCurrent()
            assertThat(session.state.value.wantsPlay).isTrue()
            session.setHostVisible(false)
            player.emit(PlayerState(wantsPlay = true, isPlaying = true, externalIntentVersion = 3))
            runCurrent()
            assertThat(player.playingCalls.last()).isFalse()
            assertThat(session.state.value.wantsPlay).isFalse()
        } finally { session.close() }
    }

}
