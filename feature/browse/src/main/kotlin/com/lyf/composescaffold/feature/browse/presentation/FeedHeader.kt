package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedSource
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.viewmodel.FeedUiState

/** 等宽模式选择和独立刷新入口，在窄屏上也不互相覆盖。 */
@Composable
internal fun FeedHeader(
    mode: FeedMode,
    state: FeedUiState,
    onMode: (FeedMode) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp).selectableGroup(),
            ) {
                FeedMode.entries.forEach { tab ->
                    val selected = tab == mode
                    Text(
                        stringResource(tab.label),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                            .selectable(selected, role = Role.Tab, onClick = { if (!selected) onMode(tab) })
                            .padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRefresh, enabled = !state.isRefreshing && !state.isInitializing) {
                Icon(Icons.Default.Refresh, stringResource(R.string.feed_refresh))
            }
        }
        Text(
            if (state.isInitializing) stringResource(R.string.feed_initial_loading) else stringResource(
                if (state.source == FeedSource.MUSEAI_LOCAL) R.string.feed_collection_local else R.string.feed_collection_public,
                state.dataList.size,
            ),
            Modifier.padding(start = 4.dp, top = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val FeedMode.label: Int
    get() = when (this) {
        FeedMode.MUSIC -> R.string.feed_music
        FeedMode.MV -> R.string.feed_mv
        FeedMode.MIXED -> R.string.feed_mixed
    }
