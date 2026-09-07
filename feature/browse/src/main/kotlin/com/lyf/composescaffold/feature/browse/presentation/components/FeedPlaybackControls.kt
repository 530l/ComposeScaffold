package com.lyf.composescaffold.feature.browse.presentation.components

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.design.ui.player.MediaSeekBar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.playback.PlaybackCommands
import kotlinx.coroutines.flow.StateFlow

/** Feed 单条作品底部播控与可拖拽进度控制栏。 */
@Composable
internal fun FeedPlaybackControls(
    mediaKey: String,
    active: Boolean,
    wantsPlay: Boolean,
    progress: StateFlow<PlaybackProgress>,
    commands: PlaybackCommands,
    onScrubbing: (Boolean) -> Unit,
) {
    val value = if (active) progress.collectAsStateWithLifecycle().value else PlaybackProgress()
    Column {
        MediaSeekBar(
            mediaKey = mediaKey,
            positionMs = value.positionMs,
            durationMs = value.durationMs,
            bufferedPositionMs = value.bufferedPositionMs,
            seekable = active && value.seekable,
            onSeek = commands::seekTo,
            accent = Color.White,
            onScrubbing = { dragging -> if (active) onScrubbing(dragging) },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End) {
            if (active && value.durationMs > 0 && !value.seekable) {
                Text(stringResource(R.string.feed_seek_unavailable), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
            }
            PlaybackToggle(active, wantsPlay, commands::toggle)
        }
    }
}

/** 播放/暂停极简矢量按键。 */
@Composable
private fun PlaybackToggle(enabled: Boolean, playing: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        if (playing && enabled) {
            val pause = stringResource(R.string.feed_pause)
            Canvas(Modifier.size(22.dp).semantics { contentDescription = pause }) {
                drawRect(Color.White, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.2f, size.height * 0.7f))
                drawRect(Color.White, Offset(size.width * 0.6f, size.height * 0.15f), Size(size.width * 0.2f, size.height * 0.7f))
            }
        } else {
            Icon(Icons.Default.PlayArrow, stringResource(R.string.feed_play), tint = Color.White)
        }
    }
}
