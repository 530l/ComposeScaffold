package com.lyf.composescaffold.feature.browse.presentation.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.model.music.MusicTrack
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.MusicHallPlayback

/** 主卡片跟随当前曲目；没有当前曲目时明确展示首曲试听入口。 */
@Composable
internal fun MusicHallBanner(
    track: MusicTrack,
    playback: MusicHallPlayback,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = track.id == playback.trackId
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                if (current) musicPlaybackLabel(playback) else stringResource(R.string.feed_start_listening),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppImage(track.coverUrl, null, Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        track.title.ifBlank { stringResource(R.string.feed_untitled) },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artist.ifBlank { stringResource(R.string.feed_unknown_artist) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(
                    when {
                        current && playback.hasError -> R.string.feed_retry
                        current && playback.wantsPlay -> R.string.feed_pause
                        current -> R.string.feed_resume_music
                        else -> R.string.feed_play
                    },
                ))
            }
        }
    }
}

@Composable
internal fun musicPlaybackLabel(playback: MusicHallPlayback): String = stringResource(
    when {
        playback.hasError -> R.string.feed_music_error
        playback.isBuffering && playback.wantsPlay -> R.string.feed_music_buffering
        playback.isPlaying -> R.string.feed_music_playing
        else -> R.string.feed_music_paused
    },
)
