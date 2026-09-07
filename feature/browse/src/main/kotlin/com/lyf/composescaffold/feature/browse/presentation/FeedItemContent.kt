package com.lyf.composescaffold.feature.browse.presentation

import com.lyf.composescaffold.core.player.playback.PlaybackProgress
import com.lyf.composescaffold.core.player.playback.VideoOutput
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
import com.lyf.composescaffold.feature.browse.R
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackState
import com.lyf.composescaffold.feature.browse.playback.PlaybackCommands
import com.lyf.composescaffold.feature.browse.presentation.components.FeedDialog
import com.lyf.composescaffold.feature.browse.presentation.components.FeedPlaybackOverlay
import com.lyf.composescaffold.feature.browse.presentation.components.FeedMusicVinylCard
import com.lyf.composescaffold.feature.browse.presentation.components.FeedVideoPlayerSurface
import kotlinx.coroutines.flow.StateFlow

/** Feed 单条作品的用户交互操作集合。 */
internal data class FeedItemActions(
    val playback: PlaybackCommands,
    val scrubbing: (Boolean) -> Unit,
    val like: () -> Unit,
    val save: () -> Unit,
)

/** 单条 Feed 作品全屏呈现容器。 */
@Composable
internal fun FeedItemContent(
    item: FeedItem,
    video: VideoOutput?,
    active: Boolean,
    playback: FeedPlaybackState,
    progress: StateFlow<PlaybackProgress>,
    interaction: FeedInteraction?,
    actions: FeedItemActions,
) {
    var dialog by remember(item.key) { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().clipToBounds().background(Color(0xFF09090C))) {
        AppImage(item.coverUrl, null, Modifier.fillMaxSize(), pixelSize = 96)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)))
        Box(Modifier.fillMaxSize().clickable(enabled = active, role = Role.Button, onClick = actions.playback::toggle)) {
            when (val media = item.media) {
                is FeedMedia.Music -> FeedMusicVinylCard(
                    item,
                    media,
                    active,
                    playback.isPlaying,
                    progress,
                ) { dialog = R.string.feed_lyrics }
                is FeedMedia.Mv -> FeedVideoPlayerSurface(item, media, video, active && playback.errorCode != null)
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.12f),
                    0.52f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.92f),
                ),
            ),
        )
        FeedItemActionsColumn(
            interaction = interaction,
            actions = actions,
            onComment = { dialog = R.string.feed_comment },
            onShare = { dialog = R.string.feed_share },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 164.dp),
        )
        FeedItemInfo(
            item = item,
            active = active,
            playback = playback,
            progress = progress,
            commands = actions.playback,
            onScrubbing = actions.scrubbing,
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (active) FeedPlaybackOverlay(playback, actions.playback::retry)
    }
    FeedDialog(item, dialog) { dialog = null }
}
