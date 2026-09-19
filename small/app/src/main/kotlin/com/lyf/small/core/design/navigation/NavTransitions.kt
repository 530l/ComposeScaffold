package com.lyf.small.core.design.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/** 转场时长：推入与退出共用，保证按钮、手势两种关闭路径节奏一致（参考 chengdongqing/WeChat）。 */
private const val TRANSITION_DURATION_MILLISECOND = 300

private val TRANSITION_ANIMATION_SPEC = tween<IntOffset>(
    durationMillis = TRANSITION_DURATION_MILLISECOND,
)

/** 推入转场：新页面从右侧全宽滑入，底层页面全宽滑出。 */
internal fun horizontalPushTransition(): ContentTransform = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
) togetherWith slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
)

/** 退出转场：底层页面从左侧全宽复位，当前页面全宽滑出；按钮关闭与侧滑关闭共用同一实现。 */
internal fun horizontalPopTransition(): ContentTransform = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
) togetherWith slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
)
