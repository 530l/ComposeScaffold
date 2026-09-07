package com.lyf.composescaffold.feature.browse.presentation

import com.lyf.composescaffold.feature.browse.playback.FeedBookmark
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedMedia
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackController
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackPolicy

/** 监听 Pager 滑屏手势并驱动会话选定与预加载。 */
@Composable
internal fun ObserveFeedPager(
    pager: PagerState,
    session: FeedPlaybackController,
    policy: FeedPlaybackPolicy,
    mode: FeedMode,
    state: FeedUiState,
    restoring: Boolean,
    appliedRevision: Int,
    resume: FeedBookmark,
    onResumeConsumed: () -> Unit,
    onIntent: (FeedIntent) -> Unit,
) {
    var pendingMusicKey by remember { mutableStateOf<String?>(null) }
    val currentState by rememberUpdatedState(state)
    val currentRestoring by rememberUpdatedState(restoring || state.revision != appliedRevision)
    val currentResume by rememberUpdatedState(resume)

    // 滚动时保留全局切歌目标，落定后优先同步，避免重新播放旧条目。
    LaunchedEffect(session, pager) {
        session.state.map { it.key }.distinctUntilChanged().collect { key ->
            if (key == null) return@collect
            val index = currentState.dataList.indexOfFirst { it.key == key }
            if (index >= 0 && currentState.dataList[index].media !is FeedMedia.Music) return@collect
            if (index != pager.settledPage || pager.isScrollInProgress) pendingMusicKey = key
        }
    }

    LaunchedEffect(session, pager) {
        snapshotFlow { Triple(pendingMusicKey, pager.isScrollInProgress, currentRestoring) }
            .collectLatest { (key, scrolling, restoringPage) ->
                if (key == null || scrolling || restoringPage) return@collectLatest
                var index = currentState.dataList.indexOfFirst { it.key == key }
                while (index < 0 && currentState.loadMoreState != LoadMoreState.End && !currentState.failed) {
                    val before = currentState
                    if (before.loadMoreState == LoadMoreState.Idle) onIntent(FeedIntent.LoadMore(mode))
                    snapshotFlow { currentState }.first { it != before }
                    index = currentState.dataList.indexOfFirst { it.key == key }
                }
                if (index >= 0) pager.scrollToPage(index)
                if (pendingMusicKey == key) pendingMusicKey = null
            }
    }

    LaunchedEffect(pager, session) {
        snapshotFlow {
            FeedPagerObservation(
                settled = pager.settledPage,
                current = pager.currentPage,
                target = pager.targetPage,
                scrolling = pager.isScrollInProgress,
                restoring = currentRestoring || pendingMusicKey != null,
                state = currentState,
            )
        }.collect { observation ->
            if (observation.restoring) return@collect
            val items = observation.state.dataList
            val selected = items.getOrNull(observation.settled) ?: return@collect
            if (!observation.scrolling) {
                val saved = currentResume.takeIf { it.key == selected.key }
                session.select(selected, saved?.positionMs ?: 0, saved?.playWhenReady ?: true)
                onResumeConsumed()
            }
            session.setPrimarilyVisible(observation.current == observation.settled)
            val neighborIndex = policy.neighbor(
                observation.settled, observation.current, observation.target, items.size,
            )
            session.preload(neighborIndex?.let(items::getOrNull))
            if (items.size - observation.settled <= 4 && observation.state.loadMoreState == LoadMoreState.Idle && !observation.state.failed) {
                onIntent(FeedIntent.LoadMore(mode))
            }
        }
    }
}

/** Pager 滚动状态瞬时快照数据模型。 */
private data class FeedPagerObservation(
    val settled: Int,
    val current: Int,
    val target: Int,
    val scrolling: Boolean,
    val restoring: Boolean,
    val state: FeedUiState,
)
