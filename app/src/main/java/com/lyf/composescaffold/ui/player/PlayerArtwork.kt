package com.lyf.composescaffold.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.image.AppImage

/** 收缩态：精致的黑胶悬浮球。 */
@Composable
internal fun FloatingMusicBubble(
    coverUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BubbleVinyl")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "VinylSpin",
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AuraBreath",
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = Color(0xCC000000),
                ambientColor = Color(0x55000000),
            )
            .clip(CircleShape)
            .background(Color(0xFF14151C)),
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFF3366).copy(alpha = auraAlpha),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.width / 2f,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF101013))
                .rotate(if (isPlaying) rotationAngle else 0f),
            contentAlignment = Alignment.Center,
        ) {
            AppImage(
                imageUrl = coverUrl,
                contentDescription = "正在播放唱片",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 16.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 0.8f),
                )
                drawCircle(
                    color = Color(0xFFE5C07B).copy(alpha = 0.9f),
                    radius = 5.dp.toPx(),
                    center = centerOffset,
                )
                drawCircle(
                    color = Color.Black,
                    radius = 2.dp.toPx(),
                    center = centerOffset,
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.5.dp.toPx()
            val diameter = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3366),
                            Color(0xFFFF5E3A),
                            Color(0xFFFF8E53),
                            Color(0xFFFF3366),
                        ),
                    ),
                    startAngle = -90f,
                    sweepAngle = (progress.coerceIn(0f, 1f)) * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B1C24))
                .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
            EqualizerWaveBars(
                isPlaying = isPlaying,
                modifier = Modifier.size(width = 10.dp, height = 9.dp),
            )
        }
    }
}

/** 展开横条左侧的微缩拟物黑胶唱片。 */
@Composable
internal fun MiniVinylRecord(
    coverUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylAura")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "VinylSpin",
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AuraBreath",
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaying) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFF416C).copy(alpha = auraAlpha),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.width / 2f,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF111114))
                .rotate(if (isPlaying) rotationAngle else 0f),
            contentAlignment = Alignment.Center,
        ) {
            AppImage(
                imageUrl = coverUrl,
                contentDescription = "唱片封面",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = 14.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 0.8f),
                )
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = 0.9f),
                    radius = 5.dp.toPx(),
                    center = centerOffset,
                )
                drawCircle(
                    color = Color.Black,
                    radius = 2.dp.toPx(),
                    center = centerOffset,
                )
            }
        }
    }
}

/** 当前播放项左侧跳动的声浪柱动画（Equalizer Wave Animation）。 */
@Composable
internal fun EqualizerWaveBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "EqualizerTransition")

    val h1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar1",
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar2",
    )
    val h3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Bar3",
    )

    Canvas(modifier = modifier) {
        val barWidth = 2.5.dp.toPx()
        val spacing = 2.5.dp.toPx()
        val maxHeight = size.height

        val heights = if (isPlaying) listOf(h1, h2, h3) else listOf(0.3f, 0.5f, 0.4f)
        val color = Color(0xFFFF5E3A)

        heights.forEachIndexed { i, factor ->
            val barH = (maxHeight * factor).coerceAtLeast(3.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = maxHeight - barH
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

/** 缓冲时展现的 3 根脉冲微声浪，替代粗暴的旋转菊花。 */
@Composable
internal fun BufferWaveIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "BufferWave")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "BufferAlpha",
    )

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val spacing = 2.dp.toPx()
        val maxHeight = size.height

        for (i in 0..2) {
            val x = i * (barWidth + spacing)
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, 2.dp.toPx()),
                size = Size(barWidth, maxHeight - 4.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
            )
        }
    }
}
