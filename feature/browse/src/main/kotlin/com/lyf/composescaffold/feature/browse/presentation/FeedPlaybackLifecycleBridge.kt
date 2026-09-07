package com.lyf.composescaffold.feature.browse.presentation

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
import com.lyf.composescaffold.feature.browse.playback.FeedPlaybackController

/** 将 Compose 界面的前后台生命周期与屏幕常亮绑定至播放会话。 */
@Composable
internal fun BindFeedLifecycle(
    session: FeedPlaybackController,
    playbackVisible: Boolean,
): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val visible by rememberUpdatedState(playbackVisible)
    val playback by session.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(session, lifecycle) {
        fun updateVisibility() {
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            session.setHostVisible(visible && resumed)
        }

        val observer = LifecycleEventObserver { _, _ -> updateVisibility() }
        lifecycle.addObserver(observer)
        updateVisibility()
        onDispose {
            lifecycle.removeObserver(observer)
            session.close()
        }
    }
    LaunchedEffect(session, playbackVisible, resumed) { session.setHostVisible(playbackVisible && resumed) }
    DisposableEffect(view, playback.isPlaying, playbackVisible) {
        val previous = view.keepScreenOn
        view.keepScreenOn = playbackVisible && playback.isPlaying
        onDispose { view.keepScreenOn = previous }
    }
    return resumed
}
