package com.lyf.composescaffold.feature.browse.presentation.components

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
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
import kotlinx.coroutines.flow.StateFlow

/** 音乐类型作品全屏呈现卡片：黑胶大碟与动态歌词高亮。 */
@Composable
internal fun FeedMusicVinylCard(
    item: FeedItem,
    media: FeedMedia.Music,
    active: Boolean,
    isPlaying: Boolean,
    progress: StateFlow<PlaybackProgress>,
    onLyrics: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 72.dp, top = 16.dp, bottom = 176.dp),
    ) {
        val recordSize = minOf(maxWidth, maxHeight * 0.72f, 320.dp).coerceAtLeast(0.dp)
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VinylDisc(
                coverUrl = item.coverUrl,
                title = item.title,
                size = recordSize,
                rotating = active && isPlaying,
            )
            Spacer(Modifier.height(24.dp))
            LyricPreview(media, active, progress, onLyrics)
        }
    }
}
