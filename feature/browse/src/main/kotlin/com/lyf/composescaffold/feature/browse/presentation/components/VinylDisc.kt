package com.lyf.composescaffold.feature.browse.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.image.AppImage

/** 拟物黑胶大碟组件。 */
@Composable
internal fun VinylDisc(
    coverUrl: String?,
    title: String,
    size: androidx.compose.ui.unit.Dp,
    rotating: Boolean,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(rotating) {
        if (rotating) {
            while (true) {
                val target = rotation.value + 360f
                rotation.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 20_000, easing = LinearEasing),
                )
            }
        }
    }

    Box(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation.value % 360f },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2A2A30), Color(0xFF151518), Color(0xFF0D0D10)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            val grooveColor = Color.White.copy(alpha = 0.04f)
            for (step in 1..8) {
                drawCircle(
                    color = grooveColor,
                    radius = radius * (0.6f + step * 0.045f),
                    center = center,
                    style = Stroke(width = 1f),
                )
            }
        }
        val coverSize = size * 0.65f
        Box(
            Modifier
                .size(coverSize)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppImage(coverUrl, title, Modifier.fillMaxSize(), pixelSize = 512)
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF09090C))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}
