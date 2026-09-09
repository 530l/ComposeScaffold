package com.lyf.composescaffold.feature.browse.presentation.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.presentation.FeedStatusOverlay
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.BrowseViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.MusicHallPlayback

@Composable
internal fun MusicHallRoute(
    viewModel: BrowseViewModel,
) {
    val mode = FeedMode.MUSIC
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val interactionError by viewModel.interactionError.collectAsStateWithLifecycle()
    val interactionPending by viewModel.interactionPending.collectAsStateWithLifecycle()
    // 投影只含曲目标识与播放阶段：大厅刻意不订阅全量播放进度，避免高频进度流把整页拖进重组。
    val playback =
        // 没有全局音乐入口时使用空展示状态。
        viewModel.musicPlaybackState?.collectAsStateWithLifecycle(initialValue = MusicHallPlayback())?.value
    Box(Modifier.fillMaxSize()) {
        // 把状态和回调交给不持有 ViewModel 的大厅组件。
        FeedMusicHallContent(
            state = state,
            playback = playback ?: MusicHallPlayback(),
            interactions = interactions,
            interactionPending = interactionPending,
            // 点击单曲时连同整个已加载队列一起提交，播放队列以当前列表为准。
            onPlayTrack = { track, queue ->
                viewModel.onIntent(
                    FeedIntent.PlayMusicTrack(
                        track,
                        queue,
                    ),
                )
            },
            onToggleLike = { viewModel.onIntent(FeedIntent.ToggleLike(it)) },
            onToggleSave = { viewModel.onIntent(FeedIntent.ToggleSave(it)) },
            onReadInteraction = { viewModel.onIntent(FeedIntent.ReadInteraction(it)) },
            onRefresh = { viewModel.onIntent(FeedIntent.Refresh(mode)) },
            onLoadMore = { viewModel.onIntent(FeedIntent.LoadMore(mode)) },
            onRetry = { viewModel.onIntent(FeedIntent.Retry(mode)) },
        )
        // restoring/settledPage 是 Pager 专用信号；大厅没有页面恢复与落定页，恒传 false/0。
        FeedStatusOverlay(state, mode, false, 0, interactionError, viewModel::onIntent)
    }
}
