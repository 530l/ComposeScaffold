package com.lyf.composescaffold.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** 具备弹性按压反馈的按钮外壳。 */
@Composable
internal fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.5f),
        label = "IconBtnScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
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
        content()
    }
}

internal fun formatTimeClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
internal fun PlayVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.15f)
            lineTo(size.width * 0.88f, size.height * 0.5f)
            lineTo(size.width * 0.22f, size.height * 0.85f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
internal fun PauseVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val barWidth = size.width * 0.26f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.16f, size.height * 0.15f),
            size = Size(barWidth, size.height * 0.7f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.58f, size.height * 0.15f),
            size = Size(barWidth, size.height * 0.7f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
        )
    }
}

@Composable
internal fun PreviousVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.1f, size.height * 0.2f),
            size = Size(size.width * 0.14f, size.height * 0.6f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
        val path = Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.2f)
            lineTo(size.width * 0.34f, size.height * 0.5f)
            lineTo(size.width * 0.9f, size.height * 0.8f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
internal fun NextVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.2f)
            lineTo(size.width * 0.66f, size.height * 0.5f)
            lineTo(size.width * 0.1f, size.height * 0.8f)
            close()
        }
        drawPath(path = path, color = color)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.76f, size.height * 0.2f),
            size = Size(size.width * 0.14f, size.height * 0.6f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
    }
}

@Composable
internal fun PlaylistVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val h = size.height * 0.13f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.22f),
            size = Size(size.width * 0.76f, h),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.44f),
            size = Size(size.width * 0.76f, h),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.66f),
            size = Size(size.width * 0.52f, h),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        )
    }
}

/** 精巧折叠/收缩向下 Chevron 矢量图标。 */
@Composable
internal fun CollapseVectorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.38f)
            lineTo(size.width * 0.50f, size.height * 0.66f)
            lineTo(size.width * 0.78f, size.height * 0.38f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
