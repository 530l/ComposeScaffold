package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackController

/** 短视频与视听信息流垂直滚动分页容器。 */
@Composable
internal fun FeedPager(
    state: FeedUiState,
    pager: PagerState,
    restoring: Boolean,
    session: FeedPlaybackController,
    interactions: Map<String, FeedInteraction>,
    onIntent: (FeedIntent) -> Unit,
) {
    val outputs by session.outputs.collectAsStateWithLifecycle()
    val playback by session.state.collectAsStateWithLifecycle()
    var scrubbing by remember(playback.key) { mutableStateOf(false) }

    if (state.dataList.isEmpty()) return

    VerticalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        key = { index -> state.dataList.getOrNull(index)?.key ?: index },
        beyondViewportPageCount = 1,
        userScrollEnabled = !restoring && !scrubbing,
    ) { index ->
        val item = state.dataList.getOrNull(index) ?: return@VerticalPager
        FeedItemContent(
            item = item,
            video = outputs[item.key],
            active = playback.key == item.key,
            playback = playback,
            progress = session.progress,
            interaction = interactions[item.key],
            actions = FeedItemActions(
                playback = session,
                scrubbing = { scrubbing = it },
                like = { onIntent(FeedIntent.ToggleLike(item.key)) },
                save = { onIntent(FeedIntent.ToggleSave(item.key)) },
            ),
        )
    }
}
