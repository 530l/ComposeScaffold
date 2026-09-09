package com.lyf.composescaffold.feature.browse.presentation.mv

import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedBookmark
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedMedia
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedUiState
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackController
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackPolicy

/**
 * 监听 Pager 滑屏手势并驱动会话选定与预加载。
 *
 * 本函数不画任何 UI：因为要用 LaunchedEffect / rememberUpdatedState 等组合作用域 API 才必须是
 * Composable，产出全部是副作用。观察拆成两个子观察器——全局切歌同步与手势选播，
 * 二者经 pendingMusicKey 门控协作。
 */
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
    onResumeConsumed: () -> Unit, // 恢复一次后清空书签，避免后续滑动重复恢复。
    onIntent: (FeedIntent) -> Unit,
) {
    // 长时间运行的收集任务读取最新列表。
    val currentState = rememberUpdatedState(state)
    // 恢复位置或刷新版本未处理时，暂缓正常选播。
    val currentRestoring = rememberUpdatedState(restoring || state.revision != appliedRevision)
    // 全局切歌后尚未同步到 Pager 的作品：切歌观察写入，选播观察读取作门控。
    val pendingMusicKey = remember { mutableStateOf<String?>(null) }

    // 全局切歌目标在滚动落定、恢复完成后同步到对应页面。
    ObserveGlobalMusicSwitch(pager, session, mode, currentState, currentRestoring, pendingMusicKey, onIntent)
    // 手势落定驱动选播、预备与提前翻页。
    ObservePagerSelection(pager, session, policy, mode, currentState, currentRestoring, pendingMusicKey, resume, onResumeConsumed, onIntent)
}

/**
 * 全局切歌与 Pager 的同步观察：滚动或恢复期间暂存目标曲目，
 * 落定后按 key 翻页找回并跳转，避免页面重新播放旧条目。
 */
@Composable
private fun ObserveGlobalMusicSwitch(
    pager: PagerState,
    session: FeedPlaybackController,
    mode: FeedMode,
    currentState: State<FeedUiState>, // 收集协程经 State 读最新列表，不随重组拿到旧引用。
    currentRestoring: State<Boolean>, // 恢复未完成时暂缓同步跳页。
    pendingMusicKey: MutableState<String?>, // 暂存的全局切歌目标，与选播观察共享。
    onIntent: (FeedIntent) -> Unit,
) {
    // 滚动时保留全局切歌目标，落定后优先同步，避免重新播放旧条目。
    LaunchedEffect(session, pager) {
        // 只关心作品变化，过滤同一作品的播放状态刷新。
        session.state.map { it.key }.distinctUntilChanged().collect { key ->
            if (key == null) return@collect
            val index = currentState.value.dataList.indexOfFirst { it.key == key }
            // 已知 MV 不参与全局音乐切歌同步。
            if (index >= 0 && currentState.value.dataList[index].media !is FeedMedia.Music) return@collect
            if (index != pager.settledPage || pager.isScrollInProgress) pendingMusicKey.value = key
        }
    }

    LaunchedEffect(session, pager) {
        snapshotFlow { Triple(pendingMusicKey.value, pager.isScrollInProgress, currentRestoring.value) }
            // collectLatest：新一轮切歌到来时取消仍在等待分页的旧查找。
            .collectLatest { (key, scrolling, restoringPage) ->
                if (key == null || scrolling || restoringPage) return@collectLatest
                var index = currentState.value.dataList.indexOfFirst { it.key == key }
                while (index < 0 && currentState.value.loadMoreState != LoadMoreState.End && !currentState.value.failed) {
                    val before = currentState.value
                    // 仅在分页空闲时请求下一页，避免重复请求。
                    if (before.loadMoreState == LoadMoreState.Idle) onIntent(FeedIntent.LoadMore(mode))
                    // 挂起等待列表或加载状态发生变化，再重新查找。
                    snapshotFlow { currentState.value }.first { it != before }
                    index = currentState.value.dataList.indexOfFirst { it.key == key }
                }
                if (index >= 0) pager.scrollToPage(index)
                // 只清除本次目标，保留后来发生的新切歌。
                if (pendingMusicKey.value == key) pendingMusicKey.value = null
            }
    }
}

/** 手势观察：落定选播当前作品、按目标页预备相邻项，临近末尾提前续页。 */
@Composable
private fun ObservePagerSelection(
    pager: PagerState,
    session: FeedPlaybackController,
    policy: FeedPlaybackPolicy,
    mode: FeedMode,
    currentState: State<FeedUiState>, // 收集协程经 State 读最新列表，不随重组拿到旧引用。
    currentRestoring: State<Boolean>, // 恢复未完成时暂缓普通选播。
    pendingMusicKey: MutableState<String?>, // 全局切歌跳页期间同样暂停普通选播。
    resume: FeedBookmark,
    onResumeConsumed: () -> Unit, // 恢复一次后清空书签，避免后续滑动重复恢复。
    onIntent: (FeedIntent) -> Unit,
) {
    // 读取最新书签，避免重复使用已消费的位置。
    val currentResume by rememberUpdatedState(resume)

    // 集中观察手势，驱动当前作品和相邻预备项。
    LaunchedEffect(pager, session) {
        snapshotFlow {
            FeedPagerObservation(
                settled = pager.settledPage,
                current = pager.currentPage,
                target = pager.targetPage,
                scrolling = pager.isScrollInProgress,
                // 书签恢复和音乐跳页期间都暂停普通选播。
                restoring = currentRestoring.value || pendingMusicKey.value != null,
                state = currentState.value,
            )
        }.collect { observation ->
            // 优先完成恢复，避免先播旧页面。
            if (observation.restoring) return@collect
            val items = observation.state.dataList
            val selected = items.getOrNull(observation.settled) ?: return@collect
            // 选播只认 settledPage：滚动落定后才切换作品，中途不打断旧页。
            if (!observation.scrolling) {
                // 只有作品相同才应用保存的进度。
                val saved = currentResume.takeIf { it.key == selected.key }
                // 恢复对应书签，否则从头播放。
                session.select(selected, saved?.positionMs ?: 0, saved?.playWhenReady ?: true)
                // 这次恢复已处理，通知页面清空书签。
                onResumeConsumed()
            }
            // 主显示页离开原作品时暂停它，手势取消后恢复。
            session.setPrimarilyVisible(observation.current == observation.settled)
            // 预备方向优先 targetPage（用户正滑向哪页），停留时按滑动方向取相邻项。
            val neighborIndex = policy.neighbor(
                observation.settled, observation.current, observation.target, items.size,
            )
            // 传 null 会取消当前预备。
            session.preload(neighborIndex?.let(items::getOrNull))
            // 距末尾不足 4 项（3 首未浏览 + 1 个加载中 footer）且分页空闲、未失败时提前续页。
            if (items.size - observation.settled <= 4 &&
                observation.state.loadMoreState == LoadMoreState.Idle && !observation.state.failed
            ) {
                onIntent(FeedIntent.LoadMore(mode))
            }
        }
    }
}

/** 把多项 Pager 状态并入一次 snapshotFlow 读取的瞬时快照，任一变化都触发选播评估。 */
private data class FeedPagerObservation(
    val settled: Int,
    val current: Int,
    val target: Int,
    val scrolling: Boolean,
    val restoring: Boolean,
    val state: FeedUiState,
)
