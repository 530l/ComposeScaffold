package com.lyf.composescaffold.feature.browse.presentation.mv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.feature.browse.presentation.mv.playback.FeedPlaybackController

/** 将 Compose 界面的前后台生命周期与屏幕常亮绑定至播放会话。 */
@Composable
internal fun BindFeedLifecycle(
    session: FeedPlaybackController,
    playbackVisible: Boolean, // 由导航和页面恢复状态共同决定的可见性。
): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    // 让生命周期回调读取最新可见性。
    val visible by rememberUpdatedState(playbackVisible)
    // 订阅实际播放状态，用于屏幕常亮。
    val playback by session.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(session, lifecycle) {
        fun updateVisibility() {
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            // 双条件缺一不可：RESUMED 挡后台，visible 挡被其他导航页遮挡；任一为假 MV 即停播。
            session.setHostVisible(visible && resumed)
        }

        // 前后台变化时立即更新，避免等待下一次重组。
        val observer = LifecycleEventObserver { _, _ -> updateVisibility() }
        lifecycle.addObserver(observer)
        // 注册后同步一次当前状态。
        updateVisibility()
        onDispose {
            lifecycle.removeObserver(observer)
            // 保存播放节点并结束页面绑定；全局音乐有独立生命周期。
            session.close()
        }
    }
    // 导航可见性单独变化时也同步播放条件。
    LaunchedEffect(session, playbackVisible, resumed) { session.setHostVisible(playbackVisible && resumed) }
    DisposableEffect(view, playback.isPlaying, playbackVisible) {
        // keepScreenOn 需链式保存-还原：记住进入本次绑定前的值。
        val previous = view.keepScreenOn
        // 仅可见且真正出声（isPlaying 而非 wantsPlay，缓冲中不常亮）时请求常亮。
        view.keepScreenOn = playbackVisible && playback.isPlaying
        onDispose { view.keepScreenOn = previous }
    }
    // 供调用方判断是否允许封面预取等页面工作。
    return resumed
}
