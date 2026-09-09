package com.lyf.composescaffold.feature.browse.presentation.mixed

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

@Composable
internal fun VinylDisc(
    coverUrl: String?,
    title: String, // 封面的无障碍描述。
    size: androidx.compose.ui.unit.Dp,
    rotating: Boolean, // 当前作品实际播放时允许旋转。
) {
    // 保留当前角度，暂停后继续时不从零开始。
    val rotation = remember { Animatable(0f) }
    // 播放条件变化时取消旧动画并重新决定是否旋转。
    LaunchedEffect(rotating) {
        if (rotating) {
            // 持续转动，依靠协程取消结束动画。
            while (true) {
                val target = rotation.value + 360f
                rotation.animateTo(
                    targetValue = target,
                    // 每圈二十秒，保持匀速。
                    animationSpec = tween(durationMillis = 20_000, easing = LinearEasing),
                )
            }
        }
    }

    Box(
        Modifier
            .size(size)
            // 在绘制层读取角度，避免旋转逐帧触发组件重组。
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
            // 纹路使用低透明度白色，避免喧宾夺主。
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
        // 中心封面占圆盘直径的六成半。
        val coverSize = size * 0.65f
        Box(
            Modifier
                .size(coverSize)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            // 封面加载前的占位图标，加载完成后被叠放的封面盖住。
            Icon(
                Icons.Default.PlayArrow,
                // 装饰性图形，不提供独立朗读说明。
                null,
                Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 封面解码上限 512px，圆内显示足够并节省内存。
            AppImage(coverUrl, title, Modifier.fillMaxSize(), pixelSize = 512)
            // 叠在封面中心的轴孔装饰，补足黑胶观感。
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
