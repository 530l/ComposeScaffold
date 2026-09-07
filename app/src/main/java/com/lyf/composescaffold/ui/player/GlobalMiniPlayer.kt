package com.lyf.composescaffold.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 全局常驻可自由拖拽移动悬浮窗播放器组件。 */
@Composable
internal fun GlobalMiniPlayer(
    modifier: Modifier = Modifier,
    visibleInCurrentTab: Boolean = true,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.playbackState.collectAsStateWithLifecycle()
    val queue by viewModel.queueState.collectAsStateWithLifecycle()
    var showPlaylistSheet by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val shouldDisplay = visibleInCurrentTab && snapshot.currentTrack != null
    if (!shouldDisplay) return
    val track = snapshot.currentTrack ?: return

    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val bubbleSizeDp = 62.dp
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }
        val marginDp = 12.dp
        val marginPx = with(density) { marginDp.toPx() }
        val bottomNavOffsetPx = with(density) { 92.dp.toPx() }
        val topStatusOffsetPx = with(density) { 60.dp.toPx() }
        val minX = marginPx
        val maxX = (screenWidthPx - bubbleSizePx - marginPx).coerceAtLeast(minX)
        val minY = topStatusOffsetPx
        val maxY = (screenHeightPx - bubbleSizePx - bottomNavOffsetPx).coerceAtLeast(minY)
        val animX = remember { Animatable(maxX) }
        val animY = remember { Animatable(maxY * 0.76f) }
        var isInitialized by remember { mutableStateOf(false) }
        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (!isInitialized && screenWidthPx > 0 && screenHeightPx > 0) {
                animX.snapTo(maxX)
                animY.snapTo((maxY * 0.76f).coerceIn(minY, maxY))
                isInitialized = true
            }
        }
        val progressRatio = if (snapshot.durationMs > 0) {
            (snapshot.positionMs.toFloat() / snapshot.durationMs).coerceIn(0f, 1f)
        } else 0f
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false },
                    ),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                initialOffsetY = { it / 2 },
            ) + scaleIn(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                initialScale = 0.92f,
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                targetOffsetY = { it / 2 },
            ) + scaleOut(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                targetScale = 0.92f,
            ) + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ExpandedFloatingMenu(
                track = track,
                snapshot = snapshot,
                progressRatio = progressRatio,
                onCollapse = { isExpanded = false },
                onShowPlaylist = { showPlaylistSheet = true },
                onPrevious = viewModel::previous,
                onToggle = viewModel::toggle,
                onNext = viewModel::next,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 86.dp),
            )
        }
        AnimatedVisibility(
            visible = !isExpanded,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
                initialScale = 0.6f,
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 480f),
                targetScale = 0.6f,
            ) + fadeOut(tween(160)),
        ) {
            var isPressed by remember { mutableStateOf(false) }
            val bubbleScale by animateFloatAsState(
                targetValue = if (isPressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
                label = "BubbleScale",
            )
            var lastTapTimestamp by remember { mutableStateOf(0L) }

            FloatingMusicBubble(
                coverUrl = track.coverUrl,
                isPlaying = snapshot.isPlaying,
                progress = progressRatio,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            animX.value.roundToInt(),
                            animY.value.roundToInt(),
                        )
                    }
                    .size(bubbleSizeDp)
                    .scale(bubbleScale)
                    .pointerInput(screenWidthPx, screenHeightPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            val startTime = System.currentTimeMillis()
                            var dragChange: PointerInputChange? = null
                            var overSlop = Offset.Zero
                            do {
                                dragChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                    change.consume()
                                    overSlop = over
                                }
                            } while (dragChange != null && !dragChange.isConsumed)

                            if (dragChange != null) {
                                val startX = (animX.value + overSlop.x).coerceIn(minX, maxX)
                                val startY = (animY.value + overSlop.y).coerceIn(minY, maxY)
                                coroutineScope.launch {
                                    animX.snapTo(startX)
                                    animY.snapTo(startY)
                                }

                                drag(dragChange.id) { change ->
                                    val delta = change.position - change.previousPosition
                                    change.consume()
                                    val newX = (animX.value + delta.x).coerceIn(minX, maxX)
                                    val newY = (animY.value + delta.y).coerceIn(minY, maxY)
                                    coroutineScope.launch {
                                        animX.snapTo(newX)
                                        animY.snapTo(newY)
                                    }
                                }

                                isPressed = false
                                val targetX = if (animX.value + bubbleSizePx / 2f < screenWidthPx / 2f) {
                                    minX
                                } else {
                                    maxX
                                }
                                coroutineScope.launch {
                                    animX.animateTo(
                                        targetValue = targetX,
                                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
                                    )
                                }
                            } else {
                                isPressed = false
                                val currentTime = System.currentTimeMillis()
                                val elapsed = currentTime - startTime
                                if (elapsed > 450 || (currentTime - lastTapTimestamp < 350)) {
                                    showPlaylistSheet = true
                                    lastTapTimestamp = 0L
                                } else {
                                    lastTapTimestamp = currentTime
                                    isExpanded = true
                                }
                            }
                        }
                    },
            )
        }
    }
    if (showPlaylistSheet) {
        MusicPlaylistBottomSheet(
            snapshot = snapshot,
            queue = queue,
            onDismiss = { showPlaylistSheet = false },
            onPlayAt = viewModel::playAt,
            onSeekTo = viewModel::seekTo,
            onSeekForward = { viewModel.seekForward() },
            onSeekRewind = { viewModel.seekRewind() },
            onCycleMode = viewModel::cyclePlayMode,
            onRemoveTrack = viewModel::removeTrack,
            onClearQueue = {
                viewModel.clearQueue()
                showPlaylistSheet = false
            },
        )
    }
}
