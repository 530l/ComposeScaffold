package com.lyf.composescaffold.feature.browse.playback.global

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
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.music.MusicPlaybackStatus
import com.lyf.composescaffold.core.model.music.MusicTrack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** 全局音乐播放器的暂停语义、错误恢复与队列更换测试。 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalMusicPlayerTest {
    private val first = MusicTrack("a", "A", "Artist", null, "https://example.com/a.mp3")
    private val second = first.copy(id = "b", title = "B", url = "https://example.com/b.mp3")

    /** 记录播放与重试调用、状态由测试手动推送的播放器替身。 */
    private class FakePlayer(override val source: MediaSource) : Player {
        override val state = MutableStateFlow(PlayerState(isBuffering = true))
        override var position = PlaybackProgress(durationMs = 60_000, seekable = true)
        override val video: VideoOutput? = null
        val playCalls = mutableListOf<Boolean>()
        var retries = 0
        override fun setPlaying(playing: Boolean) {
            playCalls.add(playing)
            state.value = state.value.copy(wantsPlay = playing, isPlaying = false)
        }
        override fun seekTo(positionMs: Long) { position = position.copy(positionMs = positionMs) }
        override fun retry() {
            retries++
            state.value = state.value.copy(errorCode = null, isBuffering = true)
        }
    }

    /** 同时充当工厂与播放器池的替身，可用 ready 阻塞建池时机。 */
    private class Factory : PlayerFactory, PlayerPool {
        override val lowRam = false
        val ready = CompletableDeferred<Unit>()
        val acquired = mutableListOf<FakePlayer>()
        val recycled = mutableListOf<Player>()
        override suspend fun createPool(capacity: Int, buffer: BufferConfig): PlayerPool {
            ready.await()
            return this
        }
        override suspend fun acquire(source: MediaSource, positionMs: Long, options: PlaybackOptions): Player =
            FakePlayer(source).also { acquired.add(it) }
        override fun recycle(player: Player) { recycled.add(player) }
        override fun release() = Unit
    }

    /** 池尚未建好时下达的暂停指令必须在就绪后仍然生效。 */
    @Test
    fun pauseBeforePlayerExists_survivesDelayedPreparation() = runTest {
        val factory = Factory()
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        player.pause()
        factory.ready.complete(Unit)
        runCurrent()
        assertThat(factory.acquired.single().playCalls).containsExactly(false)
        assertThat(player.state.value.wantsPlay).isFalse()
        player.clearQueue()
    }

    /** 缓冲中切换播放应转为暂停，而不是恢复出声。 */
    @Test
    fun toggleDuringBuffering_pausesInsteadOfResuming() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        assertThat(player.state.value.isBuffering).isTrue()
        player.toggle()
        runCurrent()
        assertThat(factory.acquired.single().playCalls.last()).isFalse()
        assertThat(player.state.value.wantsPlay).isFalse()
        player.clearQueue()
    }

    /** 恢复必须再次通过播放器播放入口执行并保持顺序。 */
    @Test
    fun resumeUsesPlayerPlaybackEntryAgain() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        player.pause()
        runCurrent()
        player.resume()
        runCurrent()
        assertThat(factory.acquired.single().playCalls).containsExactly(true, false, true).inOrder()
        player.clearQueue()
    }

    /** 失败曲目不污染下一曲，重试后清除错误继续播放。 */
    @Test
    fun failedTrackDoesNotPoisonNextTrack_andRetryClearsError() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        val fake = factory.acquired.single()
        fake.state.value = PlayerState(errorCode = 2004)
        runCurrent()
        assertThat(player.state.value.status).isEqualTo(MusicPlaybackStatus.ERROR)
        player.resume()
        runCurrent()
        assertThat(fake.retries).isEqualTo(1)
        assertThat(player.state.value.errorCode).isNull()
        fake.state.value = PlayerState(errorCode = 2004)
        runCurrent()
        player.playTrack(second)
        assertThat(player.state.value.errorCode).isNull()
        runCurrent()
        assertThat(player.state.value.currentTrack?.id).isEqualTo("b")
        assertThat(player.state.value.errorCode).isNull()
        player.clearQueue()
    }

    /** 后台启动失败要到达界面状态，并且可以被重试。 */
    @Test
    fun backgroundStartFailure_reachesUiAndCanBeRetried() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        val fake = factory.acquired.single()
        fake.state.value = PlayerState(backgroundStartFailed = true, errorCode = -2)
        runCurrent()
        assertThat(player.state.value.status).isEqualTo(MusicPlaybackStatus.ERROR)
        assertThat(player.state.value.errorCode).isEqualTo(-2)
        assertThat(player.state.value.wantsPlay).isFalse()
        player.resume()
        runCurrent()
        assertThat(fake.playCalls.last()).isTrue()
        assertThat(player.state.value.errorCode).isNull()
        player.clearQueue()
    }

    /** 用户再次暂停也要使旧的自动恢复意图失效。 */
    @Test
    fun repeatedPause_invalidatesPreviousResumeIntent() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        player.pause()
        runCurrent()
        val pausedVersion = player.state.value.intentVersion

        player.pause()
        runCurrent()
        assertThat(player.state.value.intentVersion).isGreaterThan(pausedVersion)
        assertThat(player.state.value.wantsPlay).isFalse()
        player.clearQueue()
    }

    /** 系统暂停必须改变意图版本，普通缓冲状态刷新不能。 */
    @Test
    fun externalPause_changesIntentVersion_butBufferingDoesNot() = runTest {
        val factory = Factory().also { it.ready.complete(Unit) }
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        val fake = factory.acquired.single()
        val initialVersion = player.state.value.intentVersion
        fake.state.value = fake.state.value.copy(isBuffering = false)
        runCurrent()
        assertThat(player.state.value.intentVersion).isEqualTo(initialVersion)

        // 系统暂停和普通状态刷新必须区分，否则可能错误恢复播放。
        fake.state.value = fake.state.value.copy(wantsPlay = false, externalIntentVersion = 1)
        runCurrent()
        assertThat(player.state.value.intentVersion).isGreaterThan(initialVersion)
        assertThat(player.state.value.wantsPlay).isFalse()
        player.clearQueue()
    }

    /** 连续切歌只获取最终目标播放器，并回收旧播放器。 */
    @Test
    fun replacingPendingTrack_onlyAcquiresFinalTarget() = runTest {
        val factory = Factory()
        val player = GlobalMusicPlayer(factory, backgroundScope)
        player.playTrack(first)
        runCurrent()
        player.playTrack(second)
        factory.ready.complete(Unit)
        runCurrent()
        assertThat(factory.acquired.map { it.source.id }).containsExactly("b")
        player.clearQueue()
        assertThat(factory.recycled).containsExactly(factory.acquired.single())
        assertThat(player.state.value.currentTrack).isNull()
    }
}
