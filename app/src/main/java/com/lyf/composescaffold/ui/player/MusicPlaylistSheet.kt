package com.lyf.composescaffold.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.R
import com.lyf.composescaffold.core.design.ui.player.MediaSeekBar
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicQueue
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.core.model.music.PlayMode

/** 播放列表由工具栏、进度控制和曲目列表组成。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicPlaylistBottomSheet(
    snapshot: MusicPlaybackSnapshot,
    queue: MusicQueue,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekRewind: () -> Unit,
    onCycleMode: () -> Unit,
    onRemoveTrack: (String) -> Unit,
    onClearQueue: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(queue.currentIndex) {
        if (queue.currentIndex in queue.tracks.indices) listState.animateScrollToItem(queue.currentIndex)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF14151B),
        contentColor = Color.White,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            PlaylistHeader(queue, onCycleMode, onClearQueue)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            MusicSeekControls(snapshot, onSeekTo, onSeekForward, onSeekRewind)
            Spacer(Modifier.height(10.dp))
            PlaylistTracks(queue, snapshot.isPlaying, listState, onPlayAt, onRemoveTrack)
        }
    }
}

@Composable
private fun PlaylistHeader(queue: MusicQueue, onCycleMode: () -> Unit, onClearQueue: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.player_queue_count, queue.tracks.size),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onCycleMode) {
            Text(stringResource(when (queue.playMode) {
                PlayMode.LIST_LOOP -> R.string.player_list_loop
                PlayMode.SINGLE_LOOP -> R.string.player_single_loop
                PlayMode.SHUFFLE -> R.string.player_shuffle
            }))
        }
        TextButton(onClick = onClearQueue) { Text(stringResource(R.string.player_clear_queue)) }
    }
}

@Composable
private fun MusicSeekControls(
    snapshot: MusicPlaybackSnapshot,
    onSeek: (Long) -> Unit,
    onForward: () -> Unit,
    onRewind: () -> Unit,
) {
    if (snapshot.currentTrack == null) return
    Surface(Modifier.padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFF1B1C24), contentColor = Color.White) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            MediaSeekBar(
                mediaKey = snapshot.currentTrack?.id,
                positionMs = snapshot.positionMs,
                durationMs = snapshot.durationMs,
                bufferedPositionMs = snapshot.bufferedPositionMs,
                seekable = snapshot.seekable,
                onSeek = onSeek,
                accent = Color(0xFFFF5E3A),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onRewind, enabled = snapshot.seekable) { Text(stringResource(R.string.player_rewind)) }
                TextButton(onClick = onForward, enabled = snapshot.seekable) { Text(stringResource(R.string.player_forward)) }
            }
        }
    }
}

@Composable
private fun PlaylistTracks(
    queue: MusicQueue,
    isPlaying: Boolean,
    listState: LazyListState,
    onPlayAt: (Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (queue.isEmpty) {
        Text(stringResource(R.string.player_queue_empty), Modifier.padding(24.dp))
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(320.dp)) {
        itemsIndexed(queue.tracks, key = { _, track -> track.id }) { index, track ->
            PlaylistTrackRow(track, index, index == queue.currentIndex, isPlaying,
                onPlay = { onPlayAt(index) }, onRemove = { onRemove(track.id) })
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: MusicTrack,
    index: Int,
    selected: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val removeLabel = stringResource(R.string.player_remove_track, track.title)
    Row(
        Modifier.fillMaxWidth().background(if (selected) Color(0x22FF3366) else Color.Transparent)
            .clickable(onClick = onPlay).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected) EqualizerWaveBars(isPlaying, Modifier.size(20.dp)) else Text("${index + 1}")
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color(0xFFFF5E3A) else Color.White)
            Text(track.artist.ifBlank { stringResource(R.string.player_unknown_artist) }, maxLines = 1,
                style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
        IconButton(onClick = onRemove, modifier = Modifier.semantics { contentDescription = removeLabel }) { Text("×") }
    }
}
