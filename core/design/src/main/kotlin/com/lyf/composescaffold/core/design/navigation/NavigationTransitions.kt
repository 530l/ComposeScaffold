package com.lyf.composescaffold.core.design.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

private const val NAVIGATION_TRANSITION_DURATION_MS = 500
private val NavigationTransitionEasing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)

/** 平台统一的水平推入动画（CMP 姐妹项目同款节奏）。 */
@OptIn(ExperimentalAnimationApi::class)
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.forwardContentTransform():
    ContentTransform = ContentTransform(
    targetContentEnter = slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ),
    initialContentExit = slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        targetOffset = { it / 4 },
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ) + veilOut(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ),
)

/** 平台统一的水平返回动画（CMP 姐妹项目同款节奏）。 */
@OptIn(ExperimentalAnimationApi::class)
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.popContentTransform():
    ContentTransform = ContentTransform(
    targetContentEnter = slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        initialOffset = { it / 4 },
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ) + unveilIn(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ),
    initialContentExit = slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
            easing = NavigationTransitionEasing,
        ),
    ),
)

/** 预测返回沿手势方向水平跟随，不使用 Navigation 3 默认缩放。 */
@OptIn(ExperimentalAnimationApi::class)
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.predictivePopContentTransform(
    edge: Int,
): ContentTransform {
    val direction = if (edge == NavigationEvent.EDGE_LEFT) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
    return ContentTransform(
        targetContentEnter = slideIntoContainer(
            towards = direction,
            initialOffset = { it / 4 },
            animationSpec = tween(
                durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
                easing = LinearEasing,
            ),
        ) + unveilIn(
            animationSpec = tween(
                durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
                easing = LinearEasing,
            ),
        ),
        initialContentExit = slideOutOfContainer(
            towards = direction,
            animationSpec = tween(
                durationMillis = NAVIGATION_TRANSITION_DURATION_MS,
                easing = LinearEasing,
            ),
        ),
    )
}
