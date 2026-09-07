package com.lyf.composescaffold.core.player

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.player.config.BufferConfig
import com.lyf.composescaffold.core.player.config.PlaybackOptions
import com.lyf.composescaffold.core.player.media.MediaKind
import com.lyf.composescaffold.core.player.media.MediaSource
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackConfigurationTest {
    @Test
    fun localAudioDoesNotRequireFeedMetadataOrBackgroundPlayback() {
        val source = MediaSource("local", "file:///storage/emulated/0/Music/song.mp3", MediaKind.AUDIO)
        assertThat(source.title).isEmpty()
        assertThat(PlaybackOptions().backgroundPlayback).isFalse()
        assertThat(PlaybackOptions().repeatOne).isFalse()
    }

    @Test
    fun rejectsBufferLimitsThatCannotSatisfyStartup() {
        assertThrows(IllegalArgumentException::class.java) { BufferConfig(minBufferMs = 100) }
        assertThrows(IllegalArgumentException::class.java) { BufferConfig(maxBufferMs = 100) }
        assertThrows(IllegalArgumentException::class.java) { BufferConfig(targetBytes = 0) }
    }

    @Test
    fun rejectsInvalidAspectRatioBeforeCreatingSurface() {
        for (ratio in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                MediaSource("video", "https://example.com/video.mp4", MediaKind.VIDEO, initialAspectRatio = ratio)
            }
        }
    }

    @Test
    fun playbackPolicyIsIndependentOfMediaKind() {
        val source = MediaSource("video", "https://example.com/video.mp4", MediaKind.VIDEO)
        val options = PlaybackOptions(repeatOne = false, backgroundPlayback = true)
        assertThat(source.kind).isEqualTo(MediaKind.VIDEO)
        assertThat(options.backgroundPlayback).isTrue()
        assertThat(options.repeatOne).isFalse()
    }
}
