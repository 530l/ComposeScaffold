package com.lyf.composescaffold.feature.browse.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Feed 浏览屏幕方向控制器。 */
@Composable
internal fun KeepFeedPortrait(visible: Boolean) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTabletOrFoldable = configuration.smallestScreenWidthDp >= 600
    val activity = context.findActivity()

    DisposableEffect(activity, visible, isTabletOrFoldable) {
        if (!visible || activity == null || isTabletOrFoldable) {
            return@DisposableEffect onDispose {}
        }
        val previous = activity.requestedOrientation
        try {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: IllegalStateException) {
        }

        onDispose {
            try {
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
                    previous != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ) {
                    activity.requestedOrientation = previous
                }
            } catch (_: Exception) {
            }
        }
    }
}

/** 尾递归遍历 ContextWrapper 链，安全解包找到真实的 Activity 实例 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
