package com.lyf.composescaffold.core.ui.loadmore

import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/** Android 保持 Material3 官方触发阈值。 */
internal val platformPullToRefreshThreshold: Dp
    get() = PullToRefreshDefaults.PositionalThreshold

/** Android 继续使用 Material3 默认指示器。 */
internal const val platformUsesCompactPullToRefreshIndicator: Boolean = false

/** Android 保持 Material3 官方刷新状态和动画。 */
@Composable
internal fun rememberPlatformPullToRefreshState(): PullToRefreshState =
    rememberPullToRefreshState()

/** Android 不包装列表，边界效果交给系统默认。 */
@Composable
internal fun PlatformListContainer(
    @Suppress("UNUSED_PARAMETER") pullToRefreshState: PullToRefreshState,
    content: @Composable () -> Unit,
) {
    content()
}
