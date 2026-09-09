package com.lyf.composescaffold.feature.browse.presentation.mixed

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
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.feature.browse.R
import kotlinx.coroutines.flow.StateFlow

/** 有真实时间轴时高亮当前行，否则静态显示歌词。 */
@Composable
internal fun LyricPreview(
    media: FeedMedia.Music,
    active: Boolean, // 持有页面播放权而非仅可见，决定是否订阅真实进度。
    progress: StateFlow<PlaybackProgress>, // 约 250ms 一次的高频进度流，本组件即订阅叶子。
    onLyrics: () -> Unit,
) {
    // 只有当前作品订阅时间，其他项从零位置展示。
    val position = if (active) progress.collectAsStateWithLifecycle().value.positionMs else 0L
    // 全部歌词都有真实时间戳时才启用同步。
    val timed = media.lyrics.isNotEmpty() && media.lyrics.all { it.startMs != null }
    // 找最后一行已经到达开始时间的歌词。
    val current = if (timed) media.lyrics.indexOfLast { (it.startMs ?: 0) <= position } else -1
    val preview = if (timed) {
        // 保留原始行号供高亮判断。
        media.lyrics.withIndex()
            // 保留当前行及附近行，开头位置避免负索引。
            .filter { it.index in (current - 1).coerceAtLeast(0)..(current + 1).coerceAtLeast(1) }
    } else {
        // 没有时间轴时静态显示前三行，不编造高亮。
        media.lyrics.take(3).withIndex().toList()
    }

    Column(
        Modifier
            .fillMaxWidth()
            // 让读屏服务知道歌词区域可以点击。
            .semantics { role = Role.Button }
            .clickable(onClick = onLyrics),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (media.lyrics.isEmpty()) {
            Text(
                // 区分纯音乐和普通歌词缺失。
                stringResource(if (media.instrumental) R.string.feed_instrumental else R.string.feed_no_lyrics),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            preview.forEach { line ->
                // 只有真实时间轴的当前行才高亮。
                val isCurrent = timed && line.index == current
                Text(
                    line.value.text,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(
                        alpha = 0.7f,
                    ),
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
