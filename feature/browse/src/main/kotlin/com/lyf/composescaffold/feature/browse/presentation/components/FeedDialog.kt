package com.lyf.composescaffold.feature.browse.presentation.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.feature.browse.R

/** Feed 互动与详情占位弹窗组件（FeedDialog）。 */
@Composable
internal fun FeedDialog(
    item: FeedItem,
    dialog: Int?,
    onDismiss: () -> Unit,
) {
    dialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(kind)) },
            text = {
                val lyrics = (item.media as? FeedMedia.Music)?.lyrics.orEmpty()
                Text(
                    text = if (kind == R.string.feed_lyrics) {
                        lyrics.joinToString("\n") { it.text }
                            .ifBlank { stringResource(R.string.feed_no_lyrics) }
                    } else {
                        stringResource(R.string.feed_demo_interaction)
                    },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onDismiss() },
                ) {
                    Text(stringResource(R.string.feed_close))
                }
            },
        )
    }
}
