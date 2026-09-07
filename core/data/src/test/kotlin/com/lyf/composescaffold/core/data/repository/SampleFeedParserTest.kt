package com.lyf.composescaffold.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.model.feed.FeedMedia
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class SampleFeedParserTest {
    @Test
    fun parsePublicJsonContainsMusicAndMv() {
        val file = File("src/main/assets/feed/museai-public.json")
        val jsonText = file.readText()
        val json = Json { ignoreUnknownKeys = true }
        val array = json.parseToJsonElement(jsonText)
        val items = (array as kotlinx.serialization.json.JsonArray).mapNotNull(::parseFeedSample)

        println("Parsed items count: ${items.size}")
        val musicItems = items.filter { it.media is FeedMedia.Music }
        val mvItems = items.filter { it.media is FeedMedia.Mv }
        println("Music items: ${musicItems.size}, MV items: ${mvItems.size}")

        assertThat(items).isNotEmpty()
        assertThat(musicItems).isNotEmpty()
        assertThat(mvItems).isNotEmpty()
    }

    @Test
    fun parseLocalJsonContainsMusicAndMv() {
        val file = File("../../.local/feed-assets/feed/museai.json")
        if (!file.exists()) return
        val jsonText = file.readText()
        val json = Json { ignoreUnknownKeys = true }
        val array = json.parseToJsonElement(jsonText)
        val items = (array as kotlinx.serialization.json.JsonArray).mapNotNull(::parseFeedSample)

        println("Parsed local items count: ${items.size}")
        val musicItems = items.filter { it.media is FeedMedia.Music }
        val mvItems = items.filter { it.media is FeedMedia.Mv }
        println("Local Music items: ${musicItems.size}, MV items: ${mvItems.size}")

        assertThat(items).isNotEmpty()
        assertThat(musicItems).isNotEmpty()
        assertThat(mvItems).isNotEmpty()
    }
}
