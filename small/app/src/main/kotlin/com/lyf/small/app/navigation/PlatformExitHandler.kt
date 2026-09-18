package com.lyf.small.app.navigation

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lyf.small.R

private const val EXIT_CONFIRMATION_WINDOW_MS = 2_000L

/**
 * 平台级返回键拦截与连续两次双击退出机制：
 * - 处于二级推入页时交还 NavDisplay 出栈；
 * - 处于非首页的 Tab 根节点时，按返回键切回首 Tab（探索页）；
 * - 处于首页根节点时，首次按键 Toast 提示，2 秒内再次按下则退出 Activity。
 */
@Composable
internal fun PlatformExitHandler(
    isTabRoot: Boolean,
    isHomeRoot: Boolean,
    onNavigateHome: () -> Unit,
) {
    val activity = LocalActivity.current
    val exitMessage = stringResource(R.string.press_again_to_exit)
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isTabRoot, isHomeRoot) {
        if (!isTabRoot || !isHomeRoot) lastBackPressedAt = 0L
    }

    BackHandler(enabled = isTabRoot) {
        if (!isHomeRoot) {
            onNavigateHome()
            return@BackHandler
        }
        val now = SystemClock.elapsedRealtime()
        if (lastBackPressedAt != 0L && now - lastBackPressedAt <= EXIT_CONFIRMATION_WINDOW_MS) {
            activity?.finish()
        } else {
            val currentActivity = activity ?: return@BackHandler
            lastBackPressedAt = now
            Toast.makeText(currentActivity, exitMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
