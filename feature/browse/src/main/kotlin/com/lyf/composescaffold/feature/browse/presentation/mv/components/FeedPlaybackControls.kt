package com.lyf.composescaffold.feature.browse.presentation.mv.components

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.design.ui.player.MediaSeekBar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.PlaybackCommands
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun FeedPlaybackControls(
    mediaKey: String, // 作品变化时让进度条清除旧拖动预览。
    active: Boolean, // 持有页面播放权而非仅可见，同一时刻只有一项为真。
    wantsPlay: Boolean, // 用户播放意图而非实际播放态，缓冲中也为 true 以便中途暂停。
    progress: StateFlow<PlaybackProgress>, // 进度约 250ms 更新一次，由叶子订阅，上层 collect 会重组相邻页。
    commands: PlaybackCommands,
    onScrubbing: (Boolean) -> Unit,
) {
    // 仅 active 项订阅进度，预组合的相邻页拿默认值，不随高频更新重组。
    val value = if (active) progress.collectAsStateWithLifecycle().value else PlaybackProgress()
    Column {
        MediaSeekBar(
            mediaKey = mediaKey,
            positionMs = value.positionMs,
            durationMs = value.durationMs,
            bufferedPositionMs = value.bufferedPositionMs,
            // 预览页（非 active）与不可定位媒体都禁止 Seek。
            seekable = active && value.seekable,
            onSeek = commands::seekTo,
            accent = Color.White,
            // 只透传当前项的拖动态，相邻预览页的拖动不惊动 Pager。
            onScrubbing = { dragging -> if (active) onScrubbing(dragging) },
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End) {
            // 有时长但不支持定位的当前媒体显示说明。
            if (active && value.durationMs > 0 && !value.seekable) {
                Text(stringResource(R.string.feed_seek_unavailable), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
            }
            PlaybackToggle(active, wantsPlay, commands::toggle)
        }
    }
}

@Composable
private fun PlaybackToggle(enabled: Boolean, playing: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        // 用 wantsPlay 而非实际播放态判断：缓冲中也允许按暂停。
        if (playing && enabled) {
            val pause = stringResource(R.string.feed_pause)
            Canvas(Modifier.size(22.dp).semantics { contentDescription = pause }) {
                drawRect(Color.White, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.2f, size.height * 0.7f))
                drawRect(Color.White, Offset(size.width * 0.6f, size.height * 0.15f), Size(size.width * 0.2f, size.height * 0.7f))
            }
        } else {
            Icon(Icons.Default.PlayArrow, stringResource(R.string.feed_play), tint = Color.White)
        }
    }
}
