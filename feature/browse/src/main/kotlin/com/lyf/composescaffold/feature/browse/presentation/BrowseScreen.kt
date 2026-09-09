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
import com.lyf.composescaffold.feature.browse.presentation.music.MusicHallRoute
import com.lyf.composescaffold.feature.browse.presentation.mv.FeedPagerRoute
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.BrowseViewModel
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedIntent

/** 只负责公共外观与模式路由；音乐大厅不创建 Pager 或 Feed 播放会话。 */
@Composable
internal fun BrowseScreen(
    playbackVisible: Boolean, // 宿主导航判断当前页面是否可播放。
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
    playbackOwner: FeedPlaybackViewModel = hiltViewModel(), // 与列表 ViewModel 互不持有，联动全在 FeedPagerRoute。
) {
    KeepFeedPortrait(playbackVisible)
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val state by viewModel.stateFor(mode).collectAsStateWithLifecycle()
    // Feed 内容固定使用局部深色主题，不受应用其余部分的主题设置影响。
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF8DA6),
            onPrimary = Color(0xFF4C1024),
            primaryContainer = Color(0xFF38232E),
            onPrimaryContainer = Color(0xFFFFD9E2),
            surface = Color(0xFF151318),
            surfaceContainer = Color(0xFF1C1920),
            surfaceContainerHigh = Color(0xFF29232D),
            background = Color(0xFF09090C),
        ),
    ) {
        Column(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            FeedHeader(
                mode, state,
                // 切换模式与其他用户操作一样走 Intent，不直接调用 ViewModel 方法。
                onMode = { viewModel.onIntent(FeedIntent.SelectMode(it)) },
                // 刷新只作用于当前模式的控制器，不影响其他模式已加载的列表。
                onRefresh = { viewModel.onIntent(FeedIntent.Refresh(mode)) },
            )
            Box(Modifier.weight(1f)) {
                // key 随模式切换重建子树，丢弃旧模式的滚动位置等组合状态，避免串页。
                key(mode) {
                    when (mode) {
                        FeedMode.MUSIC -> MusicHallRoute(viewModel)
                        // MV 与混合共用同一 Pager 路由和播放绑定。
                        else -> FeedPagerRoute(mode, playbackVisible, viewModel, playbackOwner)
                    }
                }
            }
        }
    }
}
