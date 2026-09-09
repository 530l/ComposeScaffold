package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.feature.browse.R

/** 三种模式共用真实互动状态，未知值不能显示为未喜欢或未收藏。 */
@Composable
internal fun FeedInteractionButtons(
    interaction: FeedInteraction?,
    busy: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (interaction == null) {
            OutlinedButton(onClick = onRead, enabled = !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (busy) R.string.feed_interaction_reading else R.string.feed_interaction_read_retry))
            }
        } else {
            FilterChip(
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                selected = interaction.liked,
                onClick = onLike,
                enabled = !busy,
                label = { Text(stringResource(if (interaction.liked) R.string.feed_liked else R.string.feed_like)) },
                leadingIcon = { Icon(Icons.Default.Favorite, null, Modifier.size(16.dp)) },
            )
            FilterChip(
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                selected = interaction.saved,
                onClick = onSave,
                enabled = !busy,
                label = { Text(stringResource(if (interaction.saved) R.string.feed_saved else R.string.feed_save)) },
                leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(16.dp)) },
            )
            if (busy) {
                val saving = stringResource(R.string.feed_interaction_saving)
                CircularProgressIndicator(
                    Modifier.size(16.dp).semantics { contentDescription = saving },
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}
