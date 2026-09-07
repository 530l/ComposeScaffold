package com.lyf.composescaffold.core.model.music

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import org.junit.Test

/**
 * 播放列表调度算法与切歌推导单元测试。
 */
class MusicQueueTest {

    private val trackA = MusicTrack(id = "1", title = "Song A", artist = "Artist A", coverUrl = null, url = "urlA")
    private val trackB = MusicTrack(id = "2", title = "Song B", artist = "Artist B", coverUrl = null, url = "urlB")
    private val trackC = MusicTrack(id = "3", title = "Song C", artist = "Artist C", coverUrl = null, url = "urlC")

    @Test
    fun emptyQueue_returnsNullForNextAndPrevious() {
        val emptyQueue = MusicQueue()
        assertThat(emptyQueue.isEmpty).isTrue()
        assertThat(emptyQueue.currentTrack).isNull()
        assertThat(emptyQueue.nextIndex()).isNull()
        assertThat(emptyQueue.previousIndex()).isNull()
    }

    @Test
    fun listLoop_advancesAndWrapsAround() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB, trackC),
            currentIndex = 0,
            playMode = PlayMode.LIST_LOOP,
        )

        assertThat(queue.currentTrack).isEqualTo(trackA)
        assertThat(queue.nextIndex()).isEqualTo(1)

        val lastQueue = queue.copy(currentIndex = 2)
        assertThat(lastQueue.nextIndex()).isEqualTo(0) // 回绕到首曲
        assertThat(lastQueue.previousIndex()).isEqualTo(1)

        val firstQueue = queue.copy(currentIndex = 0)
        assertThat(firstQueue.previousIndex()).isEqualTo(2) // 回绕到尾曲
    }

    @Test
    fun singleLoop_repeatsCurrentIndex() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB),
            currentIndex = 1,
            playMode = PlayMode.SINGLE_LOOP,
        )

        assertThat(queue.nextIndex()).isEqualTo(1)
        assertThat(queue.previousIndex()).isEqualTo(1)
    }

    @Test
    fun shuffle_picksDifferentTrackWhenMultipleExist() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB, trackC),
            currentIndex = 1,
            playMode = PlayMode.SHUFFLE,
        )

        val next = queue.nextIndex()
        assertThat(next).isNotNull()
        assertThat(next).isNotEqualTo(1)
        assertThat(next).isAtLeast(0)
        assertThat(next).isLessThan(3)
    }

    @Test
    fun singleTrack_alwaysReturnsIndexZero() {
        val singleQueue = MusicQueue(
            tracks = listOf(trackA),
            currentIndex = 0,
            playMode = PlayMode.SHUFFLE,
        )

        assertThat(singleQueue.nextIndex()).isEqualTo(0)
        assertThat(singleQueue.previousIndex()).isEqualTo(0)
    }

    @Test
    fun toMusicTrack_mapsMusicFeedItemCorrectly() {
        val feedItem = FeedItem(
            key = "music_100",
            title = "Cloud Nine",
            artist = "Vocalist",
            coverUrl = "https://example.com/cover.jpg",
            media = FeedMedia.Music(url = "https://example.com/audio.mp3"),
        )

        val track = feedItem.toMusicTrack()
        assertThat(track).isNotNull()
        assertThat(track?.id).isEqualTo("music_100")
        assertThat(track?.title).isEqualTo("Cloud Nine")
        assertThat(track?.artist).isEqualTo("Vocalist")
        assertThat(track?.coverUrl).isEqualTo("https://example.com/cover.jpg")
        assertThat(track?.url).isEqualTo("https://example.com/audio.mp3")
    }

    @Test
    fun toMusicTrack_returnsNullForMvFeedItem() {
        val mvFeedItem = FeedItem(
            key = "mv_200",
            title = "Music Video",
            artist = "Director",
            coverUrl = null,
            media = FeedMedia.Mv(url = "https://example.com/video.mp4"),
        )

        assertThat(mvFeedItem.toMusicTrack()).isNull()
    }

    @Test
    fun remove_beforeCurrent_shiftsIndexBackward() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB, trackC),
            currentIndex = 2,
            playMode = PlayMode.LIST_LOOP,
        )

        val next = queue.remove(trackA.id)
        assertThat(next).isNotNull()
        assertThat(next?.currentIndex).isEqualTo(1)
        assertThat(next?.currentTrack).isEqualTo(trackC)
    }

    @Test
    fun remove_currentTrack_movesToSamePositionOrLast() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB, trackC),
            currentIndex = 1,
            playMode = PlayMode.LIST_LOOP,
        )

        val next = queue.remove(trackB.id)
        assertThat(next).isNotNull()
        assertThat(next?.tracks).containsExactly(trackA, trackC).inOrder()
        assertThat(next?.currentIndex).isEqualTo(1)
        assertThat(next?.currentTrack).isEqualTo(trackC)
    }

    @Test
    fun remove_afterCurrent_keepsIndexUnchanged() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB, trackC),
            currentIndex = 0,
            playMode = PlayMode.LIST_LOOP,
        )

        val next = queue.remove(trackC.id)
        assertThat(next?.currentIndex).isEqualTo(0)
        assertThat(next?.currentTrack).isEqualTo(trackA)
    }

    @Test
    fun remove_lastRemainingTrack_returnsNull() {
        val queue = MusicQueue(
            tracks = listOf(trackA),
            currentIndex = 0,
            playMode = PlayMode.LIST_LOOP,
        )

        assertThat(queue.remove(trackA.id)).isNull()
    }

    @Test
    fun remove_unknownTrack_returnsSameQueue() {
        val queue = MusicQueue(
            tracks = listOf(trackA, trackB),
            currentIndex = 1,
            playMode = PlayMode.LIST_LOOP,
        )

        assertThat(queue.remove("missing")).isSameInstanceAs(queue)
    }
}
