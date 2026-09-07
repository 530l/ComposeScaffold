package com.lyf.composescaffold.feature.browse.presentation.components

import com.lyf.composescaffold.core.player.playback.VideoOutput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedOrientation

/** Feed 短视频/MV 播放画面渲染 Surface 容器。 */
@Composable
internal fun FeedVideoPlayerSurface(item: FeedItem, media: FeedMedia.Mv, video: VideoOutput?, failed: Boolean) {
    val ratio = video?.aspectRatio?.collectAsStateWithLifecycle()?.value
        ?: if (media.orientation == FeedOrientation.LANDSCAPE) 16f / 9f else 9f / 16f
    val covered = video?.covered?.collectAsStateWithLifecycle()?.value ?: true

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val width = minOf(maxWidth, maxHeight * ratio)
        Box(Modifier.width(width).aspectRatio(ratio), contentAlignment = Alignment.Center) {
            if (video != null) key(video) { video.Render(Modifier.fillMaxSize()) }
            AnimatedVisibility(
                visible = covered || failed,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xFF17171C))) {
                    AppImage(item.coverUrl, null, Modifier.fillMaxSize(), ContentScale.Fit, pixelSize = 640)
                }
            }
        }
    }
}
