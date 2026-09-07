package com.lyf.composescaffold.feature.browse.presentation

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.data.repository.FeedInteractionStore
import com.lyf.composescaffold.core.data.repository.FeedRepository
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedPage
import com.lyf.composescaffold.core.model.feed.FeedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepository : FeedRepository {
        val items = (1..5).map { id ->
            FeedItem(
                key = "item_$id",
                title = "Music $id",
                artist = "Artist $id",
                coverUrl = null,
                media = FeedMedia.Music("https://example.com/$id.mp3"),
            )
        }

        override suspend fun loadPage(mode: FeedMode, page: Int, pageSize: Int): Result<FeedPage> {
            return Result.success(FeedPage(items, hasMore = false, source = FeedSource.MUSEAI_PUBLIC))
        }
    }

    private class FakeInteractionStore : FeedInteractionStore {
        val map = mutableMapOf<String, FeedInteraction>()

        override suspend fun read(key: String): FeedInteraction = map[key] ?: FeedInteraction()

        override suspend fun write(key: String, value: FeedInteraction): Boolean {
            map[key] = value
            return true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultMode_isMixed_orRestoredFromSavedState() {
        val handle = SavedStateHandle(mapOf("feed.mode" to FeedMode.MUSIC.name))
        val viewModel = BrowseViewModel(FakeRepository(), FakeInteractionStore(), handle)
        assertThat(viewModel.mode.value).isEqualTo(FeedMode.MUSIC)
    }

    @Test
    fun toggleLike_updatesInteractionState() = runTest {
        val store = FakeInteractionStore()
        val viewModel = BrowseViewModel(FakeRepository(), store, SavedStateHandle())
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.ToggleLike("item_1"))
        advanceUntilIdle()

        assertThat(viewModel.interactions.value["item_1"]?.liked).isTrue()
        assertThat(store.map["item_1"]?.liked).isTrue()

        // 再次点击取消点赞
        viewModel.onIntent(FeedIntent.ToggleLike("item_1"))
        advanceUntilIdle()

        assertThat(viewModel.interactions.value["item_1"]?.liked).isFalse()
    }
}
