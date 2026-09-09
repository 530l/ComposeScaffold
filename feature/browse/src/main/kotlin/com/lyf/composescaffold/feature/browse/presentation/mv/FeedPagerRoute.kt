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
import com.lyf.composescaffold.feature.browse.presentation.FeedStatusOverlay
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedBookmark
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.BrowseViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import kotlinx.coroutines.launch

/**
 * MV / 混合页路由：收集状态、绑定播放会话、恢复书签（自动翻页寻回）、预取封面。
 * 装配层 —— 两个 ViewModel 互不引用，联动逻辑在此完成。
 */
@Composable
internal fun FeedPagerRoute(
    mode: FeedMode,
    playbackVisible: Boolean, // 宿主导航判断当前页面是否可播放。
    viewModel: BrowseViewModel,
    playbackOwner: FeedPlaybackViewModel,
) {
    // ===== 1. ViewModel 状态（collectAsStateWithLifecycle：进入后台即停收集）=====
    // 核心：dataList / loadMoreState / revision
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    // 已生效的点赞/收藏
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    // 交互失败提示
    val interactionError by viewModel.interactionError.collectAsStateWithLifecycle()
    // 请求中作品，防连点
    val interactionPending by viewModel.interactionPending.collectAsStateWithLifecycle()

    // ===== 2. 本地状态：书签恢复 =====
    // 供回调调挂起函数（scrollToPage）
    val scope = rememberCoroutineScope()
    // 上次断点（index/key/进度），进入时读一次
    var resume by remember { mutableStateOf(playbackOwner.bookmark(mode)) }
    // 有 key 或非第 0 页才需恢复
    val needsRestoration = resume.key != null || resume.index > 0
    // 恢复期间锁播放，防误播第 0 首
    var restoring by remember { mutableStateOf(needsRestoration) }
    // 新列表版本到达时放弃旧位置
    var appliedRevision by remember { mutableIntStateOf(state.revision) }

    // ===== 3. Pager 与播放会话 =====
    // 页数 lambda：动态查询，追加数据自动扩页
    val pager = rememberPagerState { state.dataList.size }

    // State 只建一次、重组刷新 value：session 不重建也能读到最新列表
    //（直接捕获 dataList 是陈旧闭包；当 remember 的 key 用会重建 session、打断播放）
    val latestItems by rememberUpdatedState(state.dataList)

    // bind() 仅首次执行，owner 换实例才重绑；闭包引用 latestItems 是关键
    val session = remember(playbackOwner) {
        /**
         *          // 1. 页面传入获取最新列表的函数
         *         // 2. bind 只保存这个函数
         *         //this.items = items
         *         //// 3. 以后需要列表时才调用
         *         //val currentList = this.items()
         *
         *     latestItems=    latestItemsState.value
         */
        playbackOwner.bind(mode) { latestItems }
        //等价下面写法
//        playbackOwner.bind(
//            mode = mode,
//            items = { latestItems },
//        )
    }

    // Tab 可见 && 恢复完成才起播
    val resumed = BindFeedLifecycle(session, playbackVisible && !restoring)
    // 预取落定页 ±1 封面
    if (resumed && playbackVisible && !restoring && state.dataList.isNotEmpty()) {
        val start = (pager.settledPage - 1).coerceIn(0, state.dataList.lastIndex)
        val end = (pager.settledPage + 1).coerceIn(0, state.dataList.lastIndex)
        val covers = state.dataList.slice(start..end).mapNotNull { it.coverUrl }
        PrefetchAppImages(covers)
    }

    // ===== 4. 恢复状态机：数据一变重启重查，驱动自动翻页 =====
    LaunchedEffect(
        state.dataList, state.loadMoreState, state.revision, state.failed,
        state.isInitializing, state.isRefreshing, restoring,
    ) {
        // 全新列表：重置会话与书签，重新恢复
        if (state.revision != appliedRevision) {
            session.reset()
            resume = FeedBookmark()
            restoring = true
            appliedRevision = state.revision
        }
        if (!restoring) return@LaunchedEffect
        if (state.dataList.isEmpty()) {
            // 确认无数据：清书签，展示空态
            if (!state.isInitializing && !state.isRefreshing
                && !state.failed && state.loadMoreState == LoadMoreState.End
            ) {
                resume = FeedBookmark()
                restoring = false
            }
            return@LaunchedEffect
        }
        // 优先按 key 匹配，无 key 用 index
        val savedIndex = state.dataList.indexOfFirst { it.key == resume.key }
        val targetPage =
            if (savedIndex >= 0) savedIndex
            else resume.index.coerceIn(state.dataList.indices)
        // 恢复出口,三个条件任一成立即停止翻页,故为"或":
        // ① key 已在当前列表命中,可直接定位目标页;
        // ② 无 key 的旧格式书签,保存索引已落在已加载范围内,按索引恢复即可;
        // ③ 列表已翻到末页(End)仍无此作品——确认目标不存在,按索引回退收尾,不再无限请求下一页。
        val canRestore = savedIndex >= 0 ||
            (resume.key == null && resume.index < state.dataList.size) ||
            state.loadMoreState == LoadMoreState.End
        if (canRestore) {
            // 跳到目标页，放开播放
            if (targetPage != pager.currentPage) pager.scrollToPage(targetPage)
            restoring = false
        } else if (state.loadMoreState == LoadMoreState.Idle && !state.failed) {
            // 未找到：加载下一页，到达后重启再查
            viewModel.onIntent(FeedIntent.LoadMore(mode))
        }
    }
    // 滚动联动：翻页切曲；书签消费后清空
    ObserveFeedPager(
        pager, session, playbackOwner.policy, mode,
        state, restoring, appliedRevision, resume,
        // 书签用过一次后清空，普通滑动从头播放。
        onResumeConsumed = { resume = FeedBookmark() },
        onIntent = viewModel::onIntent,
    )

    // 底层 Pager + 上层状态遮罩
    Box(Modifier.fillMaxSize()) {
        FeedPager(
            state,
            pager,
            restoring,
            session,
            interactions,
            interactionPending,
            viewModel::onIntent,
        )
        FeedStatusOverlay(
            state, mode, restoring, pager.settledPage, interactionError, viewModel::onIntent,
            onSkipRestoration = {
                // scrollToPage 是挂起函数，需协程；先回第 0 页再放开门控
                scope.launch {
                    session.reset()
                    resume = FeedBookmark()
                    if (state.dataList.isNotEmpty()) pager.scrollToPage(0)
                    restoring = false
                }
            },
        )
    }
}
