package com.lyf.composescaffold.feature.browse.presentation

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackState
import com.lyf.composescaffold.feature.browse.playback.PlaybackCommands
import com.lyf.composescaffold.feature.browse.presentation.components.FeedPlaybackControls
import kotlinx.coroutines.flow.StateFlow

/** 单条作品共用的互动按钮。 */
@Composable
internal fun FeedItemActionsColumn(
    interaction: FeedInteraction?,
    actions: FeedItemActions,
    onComment: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedAction(Icons.Default.Favorite, R.string.feed_like, interaction?.liked == true, actions.like)
        FeedAction(Icons.Default.Star, R.string.feed_save, interaction?.saved == true, actions.save)
        FeedAction(Icons.Default.Email, R.string.feed_comment) { onComment() }
        FeedAction(Icons.Default.Share, R.string.feed_share) { onShare() }
    }
}

/** 标题、作者与播控；高频进度由播控内部收集。 */
@Composable
internal fun FeedItemInfo(
    item: FeedItem,
    active: Boolean,
    playback: FeedPlaybackState,
    progress: StateFlow<PlaybackProgress>,
    commands: PlaybackCommands,
    onScrubbing: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            item.title.ifBlank { stringResource(R.string.feed_untitled) },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 52.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            item.artist.ifBlank { stringResource(R.string.feed_unknown_artist) },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 52.dp),
        )
        FeedPlaybackControls(item.key, active, playback.wantsPlay, progress, commands, onScrubbing)
    }
}

/** 竖向小圆形互动动作按钮组件。 */
@Composable
private fun FeedAction(icon: ImageVector, label: Int, selected: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.semantics { this.selected = selected }) {
            Icon(icon, stringResource(label), tint = if (selected) MaterialTheme.colorScheme.primary else Color.White)
        }
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
