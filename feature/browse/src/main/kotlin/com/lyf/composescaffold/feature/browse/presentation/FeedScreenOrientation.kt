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
    // 按最小宽度识别大屏布局，不是硬件折叠状态检测。
    val isTabletOrFoldable = configuration.smallestScreenWidthDp >= 600
    val activity = context.findActivity()

    // Activity、可见性或尺寸类型变化时重新绑定方向。
    DisposableEffect(activity, visible, isTabletOrFoldable) {
        if (!visible || activity == null || isTabletOrFoldable) {
            // 本次未修改方向，退出时无需恢复。
            return@DisposableEffect onDispose {}
        }
        // 保存进入 Feed 前的方向请求，退出时归还。
        val previous = activity.requestedOrientation
        try {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // 系统拒绝方向请求时保持当前方向。
        } catch (_: IllegalStateException) {
        }

        onDispose {
            try {
                // 只有当前仍是竖屏且进入前不是竖屏才恢复，避免覆盖其他组件新设置的请求。
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
                    previous != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ) {
                    activity.requestedOrientation = previous
                }
            // 恢复失败时结束清理，不影响页面退出。
            } catch (_: Exception) {
            }
        }
    }
}

/** 沿 ContextWrapper 向内查找 Activity，无法找到时返回空。 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
