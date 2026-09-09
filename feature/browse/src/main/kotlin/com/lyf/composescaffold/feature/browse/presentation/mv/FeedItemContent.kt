package com.lyf.composescaffold.feature.browse.presentation.mv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
import com.lyf.composescaffold.feature.browse.presentation.mixed.FeedMusicVinylCard
import com.lyf.composescaffold.feature.browse.presentation.FeedLyricsDialog
import com.lyf.composescaffold.feature.browse.presentation.mv.components.FeedPlaybackOverlay
import com.lyf.composescaffold.feature.browse.presentation.mv.components.FeedVideoPlayerSurface
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackState
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.PlaybackCommands
import kotlinx.coroutines.flow.StateFlow

internal data class FeedItemActions(
    val playback: PlaybackCommands,
    val scrubbing: (Boolean) -> Unit, // 进度拖动期间通知 Pager 暂停接管手势。
    val like: () -> Unit,
    val save: () -> Unit,
    val read: () -> Unit,
)

@Composable
internal fun FeedItemContent(
    item: FeedItem,
    video: VideoOutput?,
    active: Boolean, // 持有页面播放权而非可见，预组合的邻页无权播控。
    playback: FeedPlaybackState,
    progress: StateFlow<PlaybackProgress>, // 250ms 高频进度流，只传引用给叶子订阅，本组件不收集。
    interaction: FeedInteraction?,
    interactionPending: Boolean,
    actions: FeedItemActions,
) {
    // 按作品保存歌词弹窗状态。
    var showLyrics by remember(item.key) { mutableStateOf(false) }

    Box(Modifier
        .fillMaxSize()
        .clipToBounds()
        .background(Color(0xFF09090C))) {
        FeedItemBackdrop(item.coverUrl)
        // 只有当前播放项响应整页播放或暂停点击。
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    enabled = active,
                    role = Role.Button,
                    onClick = actions.playback::toggle,
                ),
        ) {
            when (val media = item.media) {
                is FeedMedia.Music -> FeedMusicVinylCard(
                    item,
                    media,
                    // 黑胶仅在持有播放权时才在内部订阅进度。
                    active,
                    // 传 isPlaying 而非 wantsPlay：黑胶只在真正出声时旋转。
                    playback.isPlaying,
                    progress,
                ) { showLyrics = true }
                // MV 显示视频输出，当前项失败时用封面遮住画面。
                is FeedMedia.Mv -> FeedVideoPlayerSurface(
                    item,
                    media,
                    video,
                    active && playback.errorCode != null,
                )
            }
        }
        FeedItemScrimGradient()
        FeedItemInfo(
            item = item,
            active = active,
            playback = playback,
            progress = progress,
            commands = actions.playback,
            onScrubbing = actions.scrubbing,
            interaction = interaction,
            interactionPending = interactionPending,
            actions = actions,
            onLyrics = { showLyrics = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 4.dp),
        )
        // 只有当前作品显示错误、缓冲和暂停遮罩。
        if (active) FeedPlaybackOverlay(playback, actions.playback::retry)
    }
    FeedLyricsDialog(item, showLyrics) { showLyrics = false }
}

/** 静态铺底背景：小尺寸封面加黑遮罩，位于点击层之下。 */
@Composable
private fun FeedItemBackdrop(coverUrl: String?) {
    // 用小尺寸封面铺背景，减少背景图片内存。
    AppImage(coverUrl, null, Modifier.fillMaxSize(), pixelSize = 96)
    Box(Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.72f)))
}

/** 压暗渐变：画在媒体之上、前景文字之下，只服务文字可读性。 */
@Composable
private fun FeedItemScrimGradient() {
    Box(
        // 铺满页面的渐变只负责增强文字可读性。
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.12f),
                    0.52f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.92f),
                ),
            ),
    )
}
