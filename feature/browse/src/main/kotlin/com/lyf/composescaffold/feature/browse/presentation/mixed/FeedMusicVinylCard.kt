package com.lyf.composescaffold.feature.browse.presentation.mixed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun FeedMusicVinylCard(
    item: FeedItem,
    media: FeedMedia.Music,
    active: Boolean, // 持有页面播放权而非仅可见，同一时刻只有一项为真。
    isPlaying: Boolean, // 播放器实际出声状态，与 wantsPlay 播放意图相对。
    progress: StateFlow<PlaybackProgress>, // 高频进度流由叶子按需订阅，本组件仅透传不在本层 collect。
    onLyrics: () -> Unit,
) {
    // 读取实际可用尺寸来限制黑胶大小。
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // 为底部作品信息、互动与播放控制留出空间。
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 240.dp),
    ) {
        // 直径取可用宽、高的 0.72 倍与 320dp 的最小值：大屏不过分夸张，也为歌词预览留高度。
        val recordSize = minOf(maxWidth, maxHeight * 0.72f, 320.dp).coerceAtLeast(0.dp)
        val showLyricPreview = maxHeight >= 160.dp
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VinylDisc(
                coverUrl = item.coverUrl,
                title = item.title, // 把作品标题作为封面朗读说明。
                size = recordSize,
                rotating = active && isPlaying, // 持有播放权且真实出声才转，相邻可见页不转。
            )
            if (showLyricPreview) {
                Spacer(Modifier.height(16.dp))
                // 空间足够时展示歌词预览，小窗仍可从作品信息打开完整歌词。
                LyricPreview(media, active, progress, onLyrics)
            }
        }
    }
}
