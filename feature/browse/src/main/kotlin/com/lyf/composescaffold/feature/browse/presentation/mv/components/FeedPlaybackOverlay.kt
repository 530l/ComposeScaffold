package com.lyf.composescaffold.feature.browse.presentation.mv.components

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
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackState

@Composable
internal fun BoxScope.FeedPlaybackOverlay(
    playback: FeedPlaybackState,
    onRetry: () -> Unit,
) {
    // 按错误、缓冲、暂停的优先级选择覆盖内容。
    when {
        playback.errorCode != null -> Column(
            Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.86f), RoundedCornerShape(18.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 展示脱敏错误码而非原始异常信息，避免透出内部细节。
            Text(stringResource(R.string.feed_playback_failed, playback.errorCode), color = Color.White)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.feed_retry)) }
            Text(stringResource(R.string.feed_swipe_next), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        }
        // buffering 为 true 前控制器已延迟约 300ms 确认，短暂卡顿不闪加载环。
        playback.buffering -> CircularProgressIndicator(
            Modifier.align(Alignment.Center).size(30.dp), color = Color.White, strokeWidth = 2.dp,
        )
        // 按 wantsPlay 判断而非实际播放态：仅用户主动暂停才显示播放提示。
        !playback.wantsPlay -> Icon(
            Icons.Default.PlayArrow,
            null,
            Modifier.align(Alignment.Center).size(64.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape),
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}
