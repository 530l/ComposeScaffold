package com.lyf.composescaffold.feature.browse.presentation.mv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.FeedInteractionButtons
import com.lyf.composescaffold.feature.browse.presentation.feedMediaDescription
import com.lyf.composescaffold.feature.browse.presentation.mv.components.FeedPlaybackControls
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackState
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.PlaybackCommands
import kotlinx.coroutines.flow.StateFlow

/** 作品信息、真实互动和播放控制按阅读顺序排列，不展示虚构的社交数据。 */
@Composable
internal fun FeedItemInfo(
    item: FeedItem,
    active: Boolean,
    playback: FeedPlaybackState,
    progress: StateFlow<PlaybackProgress>,
    commands: PlaybackCommands,
    onScrubbing: (Boolean) -> Unit,
    interaction: FeedInteraction?,
    interactionPending: Boolean,
    actions: FeedItemActions,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                feedMediaDescription(item.media),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if ((item.media as? FeedMedia.Music)?.lyrics?.isNotEmpty() == true) {
                TextButton(onClick = onLyrics) { Text(stringResource(R.string.feed_lyrics)) }
            }
        }
        Text(
            item.title.ifBlank { stringResource(R.string.feed_untitled) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item.artist.ifBlank { stringResource(R.string.feed_unknown_artist) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FeedInteractionButtons(
            interaction,
            interactionPending,
            actions.like,
            actions.save,
            actions.read,
        )
        FeedPlaybackControls(item.key, active, playback.wantsPlay, progress, commands, onScrubbing)
    }
}
