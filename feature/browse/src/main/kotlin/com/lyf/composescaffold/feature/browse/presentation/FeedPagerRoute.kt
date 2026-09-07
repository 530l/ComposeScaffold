package com.lyf.composescaffold.feature.browse.presentation

import com.lyf.composescaffold.feature.browse.playback.FeedBookmark
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.design.image.PrefetchAppImages
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackViewModel

/** MV / 混合页：绑定播放会话，恢复书签并观察 Pager。 */
@Composable
internal fun FeedPagerRoute(
    mode: FeedMode,
    playbackVisible: Boolean,
    viewModel: BrowseViewModel,
    playbackOwner: FeedPlaybackViewModel,
) {
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val interactionError by viewModel.interactionError.collectAsStateWithLifecycle()
    var resume by remember { mutableStateOf(playbackOwner.bookmark(mode)) }
    val needsRestoration = resume.key != null || resume.index > 0
    var restoring by remember { mutableStateOf(needsRestoration) }
    var appliedRevision by remember { mutableIntStateOf(state.revision) }
    val pager = rememberPagerState { state.dataList.size }
    val latestItems by rememberUpdatedState(state.dataList)
    val session = remember(playbackOwner) {
        playbackOwner.bind(mode) { latestItems }
    }
    val resumed = BindFeedLifecycle(session, playbackVisible && !restoring)
    if (resumed && playbackVisible && !restoring && state.dataList.isNotEmpty()) {
        val start = (pager.settledPage - 1).coerceIn(0, state.dataList.lastIndex)
        val end = (pager.settledPage + 1).coerceIn(0, state.dataList.lastIndex)
        val covers = state.dataList.slice(start..end).mapNotNull { it.coverUrl }
        PrefetchAppImages(covers)
    }
    LaunchedEffect(state.dataList.size, state.loadMoreState, state.revision, state.failed) {
        if (state.revision != appliedRevision) {
            session.reset()
            resume = FeedBookmark()
            restoring = true
            appliedRevision = state.revision
        }
        if (!restoring || state.dataList.isEmpty()) return@LaunchedEffect
        val savedIndex = state.dataList.indexOfFirst { it.key == resume.key }
        val targetPage = if (savedIndex >= 0) savedIndex else resume.index.coerceIn(state.dataList.indices)
        val canRestore = savedIndex >= 0 || resume.index < state.dataList.size || state.loadMoreState == LoadMoreState.End
        if (canRestore) {
            if (targetPage != pager.currentPage) pager.scrollToPage(targetPage)
            restoring = false
        } else if (state.loadMoreState == LoadMoreState.Idle && !state.failed) {
            viewModel.onIntent(FeedIntent.LoadMore(mode))
        }
    }
    ObserveFeedPager(
        pager, session, playbackOwner.policy, mode, state, restoring, appliedRevision, resume,
        onResumeConsumed = { resume = FeedBookmark() },
        onIntent = viewModel::onIntent,
    )

    Box(Modifier.fillMaxSize()) {
        FeedPager(state, pager, restoring, session, interactions, viewModel::onIntent)
        FeedStatusOverlay(state, mode, restoring, pager.settledPage, interactionError, viewModel::onIntent)
    }
}
