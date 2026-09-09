package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.feature.browse.R

/** 大厅和混合流共用完整歌词，只展示当前作品提供的内容。 */
@Composable
internal fun FeedLyricsDialog(item: FeedItem, visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    val media = item.media as? FeedMedia.Music ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title.ifBlank { stringResource(R.string.feed_untitled) }) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feed_lyrics), style = MaterialTheme.typography.labelLarge)
                Text(media.lyrics.joinToString("\n") { it.text }.ifBlank {
                    stringResource(if (media.instrumental) R.string.feed_instrumental else R.string.feed_no_lyrics)
                })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feed_close)) } },
    )
}
