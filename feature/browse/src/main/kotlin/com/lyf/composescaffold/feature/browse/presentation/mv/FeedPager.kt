package com.lyf.composescaffold.feature.browse.presentation.mv

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
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedUiState
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackController

/** 短视频与视听信息流垂直滚动分页容器。 */
@Composable
internal fun FeedPager(
    state: FeedUiState,
    pager: PagerState, // 滚动状态由外层恢复逻辑统一持有，恢复跳页也由它驱动。
    restoring: Boolean,
    session: FeedPlaybackController,
    interactions: Map<String, FeedInteraction>,
    interactionPending: Set<String>,
    onIntent: (FeedIntent) -> Unit,
) {
    // 订阅当前作品与预备作品的视频输出。
    val outputs by session.outputs.collectAsStateWithLifecycle()
    // 播放状态低频可在此收集；250ms 进度只以 StateFlow 下发、由叶子订阅，否则整个 Pager 随进度重组。
    val playback by session.state.collectAsStateWithLifecycle()
    // 记录进度拖动状态，换作品时清空。
    var scrubbing by remember(playback.key) { mutableStateOf(false) }

    // 空列表交给外层占位，不创建 Pager 页面。
    if (state.dataList.isEmpty()) return

    VerticalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        // 优先使用稳定作品 key，索引暂时失效时提供备用值。
        key = { index -> state.dataList.getOrNull(index)?.key ?: index },
        // 在可见范围外额外组合相邻页，页面不自行申请播放器。
        beyondViewportPageCount = 1,
        // 恢复位置或拖动进度时禁止 Pager 抢手势。
        userScrollEnabled = !restoring && !scrubbing,
    ) { index ->
        val item = state.dataList.getOrNull(index) ?: return@VerticalPager
        FeedItemContent(
            item = item,
            video = outputs[item.key],
            // active 指持有页面播放权而非可见性：beyondViewport 预组合的邻页可见但无权播控。
            active = playback.key == item.key,
            playback = playback,
            progress = session.progress,
            interaction = interactions[item.key],
            interactionPending = item.key in interactionPending,
            actions = FeedItemActions(
                playback = session,
                scrubbing = { scrubbing = it },
                like = { onIntent(FeedIntent.ToggleLike(item.key)) },
                save = { onIntent(FeedIntent.ToggleSave(item.key)) },
                read = { onIntent(FeedIntent.ReadInteraction(item.key)) },
            ),
        )
    }
}
