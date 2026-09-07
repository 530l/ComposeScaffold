package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackViewModel

/** 只负责公共外观与模式路由；音乐大厅不创建 Pager 或 Feed 播放会话。 */
@Composable
internal fun BrowseScreen(
    playbackVisible: Boolean,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
    playbackOwner: FeedPlaybackViewModel = hiltViewModel(),
) {
    KeepFeedPortrait(playbackVisible)
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFF4468), background = Color(0xFF09090C))) {
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
            FeedHeader(
                mode, state,
                onMode = { viewModel.onIntent(FeedIntent.SelectMode(it)) },
                onRefresh = { viewModel.onIntent(FeedIntent.Refresh(mode)) },
            )
            Box(Modifier.weight(1f)) {
                key(mode) {
                    when (mode) {
                        FeedMode.MUSIC -> MusicHallRoute(viewModel)
                        else -> FeedPagerRoute(mode, playbackVisible, viewModel, playbackOwner)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicHallRoute(viewModel: BrowseViewModel) {
    val mode = FeedMode.MUSIC
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    val interactions by viewModel.interactions.collectAsStateWithLifecycle()
    val interactionError by viewModel.interactionError.collectAsStateWithLifecycle()
    val playback = viewModel.musicPlaybackState?.collectAsStateWithLifecycle(initialValue = MusicHallPlayback())?.value
    Box(Modifier.fillMaxSize()) {
        FeedMusicHallContent(
            state = state,
            currentTrackId = playback?.trackId,
            isPlaying = playback?.isPlaying == true,
            interactions = interactions,
            onPlayTrack = { track, queue -> viewModel.onIntent(FeedIntent.PlayMusicTrack(track, queue)) },
            onToggleLike = { viewModel.onIntent(FeedIntent.ToggleLike(it)) },
            onRefresh = { viewModel.onIntent(FeedIntent.Refresh(mode)) },
            onLoadMore = { viewModel.onIntent(FeedIntent.LoadMore(mode)) },
            onRetry = { viewModel.onIntent(FeedIntent.Retry(mode)) },
        )
        FeedStatusOverlay(state, mode, false, 0, interactionError, viewModel::onIntent)
    }
}
