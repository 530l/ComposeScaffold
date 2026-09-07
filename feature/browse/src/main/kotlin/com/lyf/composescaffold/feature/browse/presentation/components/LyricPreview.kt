package com.lyf.composescaffold.feature.browse.presentation.components

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.feature.browse.R
import kotlinx.coroutines.flow.StateFlow

/** 动态同步歌词预览组件（当前行高亮 + 上下一行淡显）。 */
@Composable
internal fun LyricPreview(
    media: FeedMedia.Music,
    active: Boolean,
    progress: StateFlow<PlaybackProgress>,
    onLyrics: () -> Unit,
) {
    val position = if (active) progress.collectAsStateWithLifecycle().value.positionMs else 0L
    val timed = media.lyrics.isNotEmpty() && media.lyrics.all { it.startMs != null }
    val current = if (timed) media.lyrics.indexOfLast { (it.startMs ?: 0) <= position } else -1
    val preview = if (timed) {
        media.lyrics.withIndex()
            .filter { it.index in (current - 1).coerceAtLeast(0)..(current + 1).coerceAtLeast(1) }
    } else {
        media.lyrics.take(3).withIndex().toList()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onLyrics),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (media.lyrics.isEmpty()) {
            Text(
                stringResource(if (media.instrumental) R.string.feed_instrumental else R.string.feed_no_lyrics),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            preview.forEach { line ->
                val isCurrent = timed && line.index == current
                Text(
                    line.value.text,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }
    }
}
