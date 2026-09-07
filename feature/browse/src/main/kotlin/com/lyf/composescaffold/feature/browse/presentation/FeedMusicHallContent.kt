package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreFooter
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableLazyColumn
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.toMusicTrack
import com.lyf.composescaffold.feature.browse.R

/** 音乐列表仅装配推荐卡、标题和曲目行，分页交给通用容器。 */
@Composable
internal fun FeedMusicHallContent(
    state: FeedUiState,
    currentTrackId: String?,
    isPlaying: Boolean,
    interactions: Map<String, FeedInteraction>,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onToggleLike: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = remember(state.dataList) { state.dataList.mapNotNull { it.toMusicTrack() } }
    val listState = rememberLazyListState()
    var appliedRevision by remember { mutableIntStateOf(state.revision) }
    val playLoaded = { tracks.firstOrNull()?.let { onPlayTrack(it, tracks) }; Unit }
    LaunchedEffect(state.revision) {
        if (appliedRevision != state.revision) {
            listState.scrollToItem(0)
            appliedRevision = state.revision
        }
    }
    LoadableLazyColumn(
        state = listState,
        isRefreshing = state.isRefreshing,
        loadMoreState = state.loadMoreState,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        loadMoreThreshold = 4, // 3 首未浏览歌曲及 1 个 footer。
        footerContent = { LoadMoreFooter(it, onRetry = onRetry) },
        modifier = modifier.fillMaxSize().background(Color(0xFF09090C)).padding(horizontal = 16.dp),
    ) {
        item(key = "music-banner") {
            MusicHallBanner(tracks, isPlaying, playLoaded, Modifier.padding(vertical = 8.dp))
        }
        item(key = "music-header") { MusicHallHeader(tracks.size, playLoaded) }
        itemsIndexed(tracks, key = { _, track -> track.id }) { _, track ->
            MusicHallTrackRow(track, track.id == currentTrackId, isPlaying,
                interactions[track.id]?.liked == true,
                onPlay = { onPlayTrack(track, tracks) }, onLike = { onToggleLike(track.id) })
        }
    }
}

@Composable
private fun MusicHallHeader(count: Int, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.feed_music_count, count), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f))
        TextButton(onClick = onPlay, enabled = count > 0) { Text(stringResource(R.string.feed_play_loaded)) }
    }
}

@Composable
private fun MusicHallTrackRow(
    track: MusicTrack,
    selected: Boolean,
    isPlaying: Boolean,
    liked: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x22FF3366) else Color.Transparent)
            .clickable(onClick = onPlay).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MusicHallCover(track.coverUrl, selected, isPlaying)
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFFFF5E3A) else Color.White)
            Text(track.artist.ifBlank { stringResource(R.string.feed_unknown_artist) },
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onLike) {
            Icon(Icons.Default.Favorite, stringResource(R.string.feed_like),
                tint = if (liked) Color(0xFFFF3366) else Color.Gray)
        }
    }
}

@Composable
private fun MusicHallCover(url: String?, selected: Boolean, isPlaying: Boolean) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        AppImage(url, null, Modifier.fillMaxSize())
        if (selected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            MusicHallWaveBars(isPlaying, Modifier.size(16.dp))
        }
    }
}
