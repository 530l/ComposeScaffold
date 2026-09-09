package com.lyf.composescaffold.feature.browse.presentation.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreFooter
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableLazyColumn
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.toMusicTrack
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.FeedInteractionButtons
import com.lyf.composescaffold.feature.browse.presentation.FeedLyricsDialog
import com.lyf.composescaffold.feature.browse.presentation.feedMediaDescription
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedUiState
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.MusicHallPlayback

/** 封面、曲目信息和互动都来自已加载作品，分页仍由通用容器驱动。 */
@Composable
internal fun FeedMusicHallContent(
    state: FeedUiState,
    playback: MusicHallPlayback,
    interactions: Map<String, FeedInteraction>,
    interactionPending: Set<String>,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onReadInteraction: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = remember(state.dataList) { state.dataList.mapNotNull { it.toMusicTrack() } }
    val featured = tracks.firstOrNull { it.id == playback.trackId } ?: tracks.firstOrNull()
    val listState = rememberLazyListState()
    var appliedRevision by remember { mutableIntStateOf(state.revision) }
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
        loadMoreThreshold = 4,
        footerContent = { LoadMoreFooter(it, onRetry = onRetry) },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        if (featured != null) {
            item(key = "music-banner") {
                MusicHallBanner(
                    featured, playback,
                    onPlay = { onPlayTrack(featured, tracks) },
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        item(key = "music-header") {
            Text(
                stringResource(R.string.feed_music_count, tracks.size),
                Modifier.padding(top = 8.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.dataList, key = { it.key }) { item ->
            val track = item.toMusicTrack() ?: return@items
            MusicHallTrackCard(
                item, playback, interactions[item.key], item.key in interactionPending,
                onPlay = { onPlayTrack(track, tracks) },
                onLike = { onToggleLike(item.key) },
                onSave = { onToggleSave(item.key) },
                onRead = { onReadInteraction(item.key) },
            )
        }
    }
}

@Composable
private fun MusicHallTrackCard(
    item: FeedItem,
    playback: MusicHallPlayback,
    interaction: FeedInteraction?,
    busy: Boolean,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onRead: () -> Unit,
) {
    var showLyrics by remember(item.key) { mutableStateOf(false) }
    val selected = item.key == playback.trackId
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppImage(
                    item.coverUrl, null,
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title.ifBlank { stringResource(R.string.feed_untitled) },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.artist.ifBlank { stringResource(R.string.feed_unknown_artist) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (selected) musicPlaybackLabel(playback) else feedMediaDescription(item.media),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onPlay) {
                    if (selected && playback.wantsPlay && !playback.hasError) {
                        Text(
                            stringResource(R.string.feed_pause),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            stringResource(if (selected && playback.hasError) R.string.feed_retry else R.string.feed_play),
                        )
                    }
                }
            }
            FeedInteractionButtons(interaction, busy, onLike, onSave, onRead)
            if ((item.media as? FeedMedia.Music)?.lyrics?.isNotEmpty() == true) {
                TextButton(
                    onClick = {
                        showLyrics = true
                    },
                ) { Text(stringResource(R.string.feed_lyrics)) }
            }
        }
    }
    FeedLyricsDialog(item, showLyrics) { showLyrics = false }
}
