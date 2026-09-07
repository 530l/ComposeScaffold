package com.lyf.composescaffold.feature.browse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackState

/** Feed 播放状态居中覆盖物（异常重试卡片、缓冲指示器、手动暂停大图标）。 */
@Composable
internal fun BoxScope.FeedPlaybackOverlay(playback: FeedPlaybackState, onRetry: () -> Unit) {
    when {
        playback.errorCode != null -> Column(
            Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.86f), RoundedCornerShape(18.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.feed_playback_failed, playback.errorCode), color = Color.White)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.feed_retry)) }
            Text(stringResource(R.string.feed_swipe_next), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        }
        playback.buffering -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(30.dp), color = Color.White, strokeWidth = 2.dp)
        !playback.wantsPlay -> Icon(
            Icons.Default.PlayArrow,
            null,
            Modifier.align(Alignment.Center).size(64.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape),
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}
