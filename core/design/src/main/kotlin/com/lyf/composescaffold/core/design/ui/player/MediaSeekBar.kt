package com.lyf.composescaffold.core.design.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

/** 拖动时本地预览，松手提交；切换作品或取消手势时丢弃预览。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSeekBar(
    mediaKey: String?,
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    seekable: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onScrubbing: (Boolean) -> Unit = {},
) = key(mediaKey) {
    var preview by remember { mutableStateOf<Float?>(null) }
    val interactions = remember { MutableInteractionSource() }
    val dragging by interactions.collectIsDraggedAsState()
    val scrubbing by rememberUpdatedState(onScrubbing)
    DisposableEffect(dragging) {
        scrubbing(dragging)
        onDispose { scrubbing(false) }
    }
    LaunchedEffect(dragging) { if (!dragging) preview = null }
    val fraction = (preview ?: if (durationMs > 0) positionMs.toFloat() / durationMs else 0f).coerceIn(0f, 1f)
    val buffered = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Column(modifier) {
        Slider(
            value = fraction,
            onValueChange = { preview = it },
            onValueChangeFinished = {
                preview?.let { onSeek((it * durationMs).toLong()) }
                preview = null
            },
            enabled = seekable && durationMs > 0,
            interactionSource = interactions,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            thumb = { Box(Modifier.size(if (dragging) 14.dp else 8.dp).background(accent, CircleShape)) },
            track = {
                Canvas(Modifier.fillMaxWidth().height(if (dragging) 4.dp else 2.dp)) {
                    drawRect(accent.copy(alpha = 0.16f))
                    drawRect(accent.copy(alpha = 0.3f), size = Size(size.width * buffered, size.height))
                    drawRect(accent, size = Size(size.width * fraction, size.height))
                }
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMediaTime((fraction * durationMs).toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatMediaTime(durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun formatMediaTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0) / 1_000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
