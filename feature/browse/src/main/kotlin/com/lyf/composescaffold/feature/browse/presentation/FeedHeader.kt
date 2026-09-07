package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.feature.browse.R

/** Feed 页面顶部流模式切换与操作工具栏。 */
@Composable
internal fun FeedHeader(mode: FeedMode, state: FeedUiState, onMode: (FeedMode) -> Unit, onRefresh: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FeedMode.entries.forEach { tab ->
                val selected = tab == mode
                Column(
                    Modifier.semantics { this.selected = selected }
                        .clickable(role = Role.Tab) { if (!selected) onMode(tab) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    )
                    Box(Modifier.size(18.dp, 2.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape))
                }
            }
        }
        IconButton(onClick = onRefresh, enabled = !state.isRefreshing && !state.isInitializing, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Default.Refresh, stringResource(R.string.feed_refresh), tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
        }
    }
}

/** 模式文案国际化资源映射 */
private val FeedMode.label: Int
    get() = when (this) {
        FeedMode.MUSIC -> R.string.feed_music
        FeedMode.MV -> R.string.feed_mv
        FeedMode.MIXED -> R.string.feed_mixed
    }
