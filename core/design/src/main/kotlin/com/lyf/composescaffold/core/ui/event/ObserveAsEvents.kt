package com.lyf.composescaffold.core.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * 一次性事件（one-shot event）的标准收集通道。
 *
 * 与 UiState 互补：状态回答"页面现在长什么样"（可重放、幂等，用
 * collectAsStateWithLifecycle 收集）；事件回答"刚刚发生了什么"——导航跳转、
 * toast、snackbar 这类不该被重组或状态恢复重放的信号，由 ViewModel 经
 * Channel/SharedFlow 发射，UI 侧统一用本函数收集。
 *
 * - 生命周期安全：仅在 [Lifecycle.State.STARTED] 及以上收集，页面不可见时挂起，
 *   回到前台继续；回调取最新组合实例，重组不会丢失在途事件。
 * - Nav3 场景语义：entry 内的 [LocalLifecycleOwner] 是 scene lifecycle，
 *   页面被推入页覆盖时自动暂停收集，底层页面不会偷跑事件。
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { event ->
                currentOnEvent(event)
            }
        }
    }
}
