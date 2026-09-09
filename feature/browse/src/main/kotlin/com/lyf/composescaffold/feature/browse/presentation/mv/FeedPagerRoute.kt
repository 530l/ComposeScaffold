package com.lyf.composescaffold.feature.browse.presentation.mv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.design.image.PrefetchAppImages
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.BrowseViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import com.lyf.composescaffold.feature.browse.presentation.FeedStatusOverlay
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedBookmark
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackViewModel

import kotlinx.coroutines.launch

/** MV / 混合页：绑定播放会话，恢复书签并观察 Pager。 */
@Composable
internal fun FeedPagerRoute(
    mode: FeedMode,
    playbackVisible: Boolean, // 宿主导航判断当前页面是否可播放。
    viewModel: BrowseViewModel,
    playbackOwner: FeedPlaybackViewModel,
) {
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val interactionError by viewModel.interactionError.collectAsStateWithLifecycle()
    val interactionPending by viewModel.interactionPending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var resume by remember { mutableStateOf(playbackOwner.bookmark(mode)) }
    val needsRestoration = resume.key != null || resume.index > 0
    // 恢复完成前阻止正常选播。
    var restoring by remember { mutableStateOf(needsRestoration) }
    var appliedRevision by remember { mutableIntStateOf(state.revision) }
    val pager = rememberPagerState { state.dataList.size }
    // 让播放绑定中的回调能读取分页后的新列表。
    val latestItems by rememberUpdatedState(state.dataList)
    // bind 返回 this：session 就是 playbackOwner 本身；按 owner 记忆避免重复绑定。
    val session = remember(playbackOwner) {
        playbackOwner.bind(mode) { latestItems }
    }
    // 绑定生命周期，书签未恢复时先不允许 MV 起播。
    val resumed = BindFeedLifecycle(session, playbackVisible && !restoring)
    // 页面稳定后预取落定页前后各一项的封面，预取窗口保持最小。
    if (resumed && playbackVisible && !restoring && state.dataList.isNotEmpty()) {
        val start = (pager.settledPage - 1).coerceIn(0, state.dataList.lastIndex)
        val end = (pager.settledPage + 1).coerceIn(0, state.dataList.lastIndex)
        val covers = state.dataList.slice(start..end).mapNotNull { it.coverUrl }
        PrefetchAppImages(covers)
    }
    LaunchedEffect(
        state.dataList, state.loadMoreState, state.revision, state.failed,
        state.isInitializing, state.isRefreshing, restoring,
    ) {
        // revision 只在成功刷新且列表引用变化时递增；出现新版本即放弃旧位置回首页。
        if (state.revision != appliedRevision) {
            // 保存旧检查点并清空当前播放选择。
            session.reset()
            resume = FeedBookmark()
            restoring = true
            appliedRevision = state.revision
        }
        if (!restoring) return@LaunchedEffect
        if (state.dataList.isEmpty()) {
            // 已确认空结果时退出恢复，展示空态而非永久停在查找面板。
            if (!state.isInitializing && !state.isRefreshing && !state.failed && state.loadMoreState == LoadMoreState.End) {
                resume = FeedBookmark()
                restoring = false
            }
            return@LaunchedEffect
        }
        // 优先通过稳定 key 找回原作品。
        val savedIndex = state.dataList.indexOfFirst { it.key == resume.key }
        val targetPage = if (savedIndex >= 0) savedIndex else resume.index.coerceIn(state.dataList.indices)
        // 有 key 时必须查到作品或穷尽列表；仅无 key 的旧书签可以直接按索引恢复。
        val canRestore = savedIndex >= 0 ||
            (resume.key == null && resume.index < state.dataList.size) ||
            state.loadMoreState == LoadMoreState.End
        if (canRestore) {
            if (targetPage != pager.currentPage) pager.scrollToPage(targetPage)
            restoring = false
        } else if (state.loadMoreState == LoadMoreState.Idle && !state.failed) {
            // 通过列表业务请求下一页，不在 UI 直接访问仓库。
            viewModel.onIntent(FeedIntent.LoadMore(mode))
        }
    }
    // 两个 ViewModel 互不持有对方，滚动联动、分页与恢复全部在本路由装配。
    ObserveFeedPager(
        pager, session, playbackOwner.policy, mode,
        state, restoring, appliedRevision, resume,
        // 书签用过一次后清空，普通滑动从头播放。
        onResumeConsumed = { resume = FeedBookmark() },
        onIntent = viewModel::onIntent,
    )

    Box(Modifier.fillMaxSize()) {
        FeedPager(state, pager, restoring, session, interactions, interactionPending, viewModel::onIntent)
        FeedStatusOverlay(
            state, mode, restoring, pager.settledPage, interactionError, viewModel::onIntent,
            onSkipRestoration = {
                scope.launch {
                    // 用户主动放弃旧书签，先定位再放开选播，避免短暂播放错误页面。
                    session.reset()
                    resume = FeedBookmark()
                    if (state.dataList.isNotEmpty()) pager.scrollToPage(0)
                    restoring = false
                }
            },
        )
    }
}
