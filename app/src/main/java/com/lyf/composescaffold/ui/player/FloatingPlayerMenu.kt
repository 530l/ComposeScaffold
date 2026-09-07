package com.lyf.composescaffold.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.model.music.MusicPlaybackSnapshot
import com.lyf.composescaffold.core.model.music.MusicPlaybackStatus
import com.lyf.composescaffold.core.model.music.MusicTrack

/** 悬浮卡片：歌曲信息、播控与轻量进度展示。 */
@Composable
internal fun ExpandedFloatingMenu(
    track: MusicTrack,
    snapshot: MusicPlaybackSnapshot,
    progressRatio: Float,
    onCollapse: () -> Unit,
    onShowPlaylist: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x99000000),
                ambientColor = Color(0x44000000),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF015161D))
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20f) {
                        onCollapse()
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniVinylRecord(
                    coverUrl = track.coverUrl,
                    isPlaying = snapshot.isPlaying,
                    onClick = onShowPlaylist,
                )

                Spacer(modifier = Modifier.width(12.dp))
                FloatingTrackInfo(track, onShowPlaylist, Modifier.weight(1f))

                Spacer(modifier = Modifier.width(4.dp))
                FloatingPlaybackActions(snapshot, onPrevious, onToggle, onNext, onShowPlaylist, onCollapse)
            }
            LuminousProgressBar(
                progress = progressRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }
    }
}

/** 流光渐变发光进度条组件。 */
@Composable
internal fun LuminousProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val activeWidth = width * progress.coerceIn(0f, 1f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            size = Size(width, height),
            cornerRadius = CornerRadius(height / 2f, height / 2f),
        )

        if (activeWidth > 0) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFF3366),
                        Color(0xFFFF5E3A),
                        Color(0xFFFF8E53),
                    ),
                    startX = 0f,
                    endX = width,
                ),
                size = Size(activeWidth, height),
                cornerRadius = CornerRadius(height / 2f, height / 2f),
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(activeWidth, height / 2f),
            )
            drawCircle(
                color = Color(0xFFFF5E3A).copy(alpha = 0.5f),
                radius = 4.dp.toPx(),
                center = Offset(activeWidth, height / 2f),
            )
        }
    }
}

/** 带有按压微缩放手势反馈的圆形播放/暂停主按键。 */
@Composable
internal fun PlayPauseActionButton(
    status: MusicPlaybackStatus,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "BtnScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(38.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF3366), Color(0xFFFF5E3A)),
                ),
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (status == MusicPlaybackStatus.BUFFERING) {
            BufferWaveIndicator(
                color = Color.White,
                modifier = Modifier.size(16.dp),
            )
        } else if (isPlaying) {
            PauseVectorIcon(
                color = Color.White,
                modifier = Modifier.size(14.dp),
            )
        } else {
            PlayVectorIcon(
                color = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FloatingTrackInfo(track: MusicTrack, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        Text(track.title, style = MaterialTheme.typography.titleSmall, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(track.artist.ifBlank { androidx.compose.ui.res.stringResource(com.lyf.composescaffold.R.string.player_unknown_artist) },
            style = MaterialTheme.typography.bodySmall, color = Color.LightGray,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FloatingPlaybackActions(
    snapshot: MusicPlaybackSnapshot,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onShowPlaylist: () -> Unit,
    onCollapse: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AnimatedIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(34.dp),
        ) {
            PreviousVectorIcon(
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp),
            )
        }
        PlayPauseActionButton(
            status = snapshot.status,
            isPlaying = snapshot.wantsPlay,
            onClick = onToggle,
        )
        AnimatedIconButton(
            onClick = onNext,
            modifier = Modifier.size(34.dp),
        ) {
            NextVectorIcon(
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp),
            )
        }
        AnimatedIconButton(
            onClick = onShowPlaylist,
            modifier = Modifier.size(34.dp),
        ) {
            PlaylistVectorIcon(
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp),
            )
        }
        AnimatedIconButton(
            onClick = onCollapse,
            modifier = Modifier.size(34.dp),
        ) {
            CollapseVectorIcon(
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
