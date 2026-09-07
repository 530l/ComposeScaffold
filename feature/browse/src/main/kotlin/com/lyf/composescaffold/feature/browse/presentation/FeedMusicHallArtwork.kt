package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyf.composescaffold.core.design.image.AppImage
import com.lyf.composescaffold.core.model.music.MusicTrack

/** 顶部精选黑胶大碟 Banner 卡片。 */
@Composable
internal fun MusicHallBanner(
    tracks: List<MusicTrack>,
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topTrack = tracks.firstOrNull()
    val infiniteTransition = rememberInfiniteTransition(label = "BannerSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "BannerAngle",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF261D22),
                        Color(0xFF1B1720),
                        Color(0xFF14131A),
                    ),
                ),
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.lyf.composescaffold.feature.browse.R.string.feed_music_radar),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5E3A),
                        letterSpacing = 1.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = topTrack?.title ?: androidx.compose.ui.res.stringResource(com.lyf.composescaffold.feature.browse.R.string.feed_recommended_playlist),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topTrack?.artist.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.Button(onClick = onPlayAll) {
                    Text(androidx.compose.ui.res.stringResource(com.lyf.composescaffold.feature.browse.R.string.feed_play_loaded))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0F12))
                    .rotate(if (isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center,
            ) {
                AppImage(
                    imageUrl = topTrack?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = 42.dp.toPx(),
                        center = centerOffset,
                    )
                    drawCircle(
                        color = Color(0xFFD4AF37),
                        radius = 8.dp.toPx(),
                        center = centerOffset,
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 4.dp.toPx(),
                        center = centerOffset,
                    )
                }
            }
        }
    }
}

/** 列表项上的跳动声浪小柱。 */
@Composable
internal fun MusicHallWaveBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "HallWave")
    val h1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "H1",
    )
    val h2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "H2",
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "H3",
    )

    Canvas(modifier = modifier) {
        val barW = 2.dp.toPx()
        val spacing = 2.dp.toPx()
        val maxH = size.height
        val heights = if (isPlaying) listOf(h1, h2, h3) else listOf(0.4f, 0.6f, 0.4f)
        val color = Color(0xFFFF5E3A)

        heights.forEachIndexed { i, factor ->
            val barH = (maxH * factor).coerceAtLeast(2.dp.toPx())
            val x = i * (barW + spacing)
            val y = maxH - barH
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
