package com.lyf.composescaffold.core.design.ui.loadmore

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 距底部剩余可见项数达到该值即预加载下一页。 */
const val DEFAULT_LOAD_MORE_THRESHOLD = 5

/**
 * 触底加载检测。只在用户向列表末尾滚动且 [LoadMoreState.Idle] 时触发：
 * Loading 由状态机去重、Failed 不自动重试（等用户点击 footer）、End 直接收口。
 * `totalItemsCount > threshold` 保证首屏不满一屏的短列表不会误触发。
 * 加载状态本身不参与触发信号，避免 Loading → Idle 时仍停在阈值区而连续请求下一页。
 */
@Composable
fun LoadMoreTrigger(
    state: LazyListState,
    loadMoreState: LoadMoreState,
    onLoadMore: () -> Unit,
    threshold: Int = DEFAULT_LOAD_MORE_THRESHOLD,
) {
    val currentLoadMoreState by rememberUpdatedState(loadMoreState)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, threshold) {
        snapshotFlow {
            val info = state.layoutInfo
            val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val remainCount = info.totalItemsCount - lastVisibleIndex - 1
            LoadMoreSignal(
                inLoadMoreZone = info.totalItemsCount > threshold && remainCount <= threshold,
                scrollingForward = state.isScrollInProgress && state.lastScrolledForward,
            )
        }
            .distinctUntilChanged()
            .collect { signal ->
                if (
                    signal.inLoadMoreZone &&
                    signal.scrollingForward &&
                    currentLoadMoreState == LoadMoreState.Idle
                ) {
                    currentOnLoadMore()
                }
            }
    }
}

/** [LoadMoreTrigger] 的网格列表版本。 */
@Composable
fun LoadMoreTrigger(
    state: LazyGridState,
    loadMoreState: LoadMoreState,
    onLoadMore: () -> Unit,
    threshold: Int = DEFAULT_LOAD_MORE_THRESHOLD,
) {
    val currentLoadMoreState by rememberUpdatedState(loadMoreState)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state, threshold) {
        snapshotFlow {
            val info = state.layoutInfo
            val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val remainCount = info.totalItemsCount - lastVisibleIndex - 1
            LoadMoreSignal(
                inLoadMoreZone = info.totalItemsCount > threshold && remainCount <= threshold,
                scrollingForward = state.isScrollInProgress && state.lastScrolledForward,
            )
        }
            .distinctUntilChanged()
            .collect { signal ->
                if (
                    signal.inLoadMoreZone &&
                    signal.scrollingForward &&
                    currentLoadMoreState == LoadMoreState.Idle
                ) {
                    currentOnLoadMore()
                }
            }
    }
}

private data class LoadMoreSignal(
    val inLoadMoreZone: Boolean,
    val scrollingForward: Boolean,
)
