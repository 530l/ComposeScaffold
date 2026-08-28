package com.lyf.composescaffold.navigation

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
import com.lyf.composescaffold.R

private const val EXIT_CONFIRMATION_WINDOW_MS = 2_000L

/** 非 Tab 根节点交还系统返回；Tab 根节点：非首页回首页，首页首次提示、再次返回才退出。 */
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
