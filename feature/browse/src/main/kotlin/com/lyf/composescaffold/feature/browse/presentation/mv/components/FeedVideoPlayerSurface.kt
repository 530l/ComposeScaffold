package com.lyf.composescaffold.feature.browse.presentation.mv.components

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
import com.lyf.composescaffold.core.player.playback.VideoOutput

@Composable
internal fun FeedVideoPlayerSurface(
    item: FeedItem,
    media: FeedMedia.Mv,
    video: VideoOutput?,
    failed: Boolean, // 当前项播放失败时保持封面遮住画面。
) {
    val ratio = video?.aspectRatio?.collectAsStateWithLifecycle()?.value
    // 播放器尚未报告比例时按媒体方向预留，避免首帧前按错误比例布局。
        ?: if (media.orientation == FeedOrientation.LANDSCAPE) 16f / 9f else 9f / 16f
    // covered 三态：无输出即视为盖住，首帧到达才为 false，出错后重新盖住。
    val covered = video?.covered?.collectAsStateWithLifecycle()?.value ?: true

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 宽高双向取小做 aspect-fit，画面按真实比例完整显示不裁切。
        val width = minOf(maxWidth, maxHeight * ratio)
        // 画面与封面共用同一比例容器，封面淡出时几何一致不跳变。
        Box(Modifier
            .width(width)
            .aspectRatio(ratio), contentAlignment = Alignment.Center) {
            // 输出对象变化时重建渲染绑定，UI 不创建播放器。
            if (video != null) key(video) { video.Render(Modifier.fillMaxSize()) }
            AnimatedVisibility(
                visible = covered || failed,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                // 封面未加载时仍保留深色底色。
                Box(Modifier
                    .fillMaxSize()
                    .background(Color(0xFF17171C))) {
                    // 封面只服务首帧过渡，解码上限 640px 兼顾清晰与内存。
                    AppImage(
                        item.coverUrl,
                        null,
                        Modifier.fillMaxSize(),
                        ContentScale.Fit,
                        pixelSize = 640,
                    )
                }
            }
        }
    }
}
