package com.lyf.composescaffold.core.design.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * 在 STARTED 及以上收集 Flow，用最新回调处理导航、提示等副作用。
 *
 * 低于 STARTED 时取消收集，恢复后重新订阅。是否保留、重放事件由上游 Flow 决定，
 * 本函数不保证事件不丢失或只处理一次；传入 StateFlow 时，回调需允许重复执行。
 *
 * 使用当前 LocalLifecycleOwner，不额外判断页面是否被覆盖。
 * 展示状态用 collectAsStateWithLifecycle；播放器释放和滚动同步各自管理生命周期。
 */
@Composable
fun <T> ObserveAsEvents(
    // 要监听的事件流；缓冲与重放规则由发送方决定。
    flow: Flow<T>,
    // 收到事件后执行的界面操作。
    onEvent: (T) -> Unit,
) {
    // 使用当前页面提供的生命周期。
    val lifecycleOwner = LocalLifecycleOwner.current
    // 保留最新回调，回调变化时不必重启收集。
    val currentOnEvent by rememberUpdatedState(onEvent)

    // 流或生命周期所属对象变化时重启；离开组合时取消。
    LaunchedEffect(flow, lifecycleOwner) {
        // 进入 STARTED 后订阅，低于 STARTED 时取消订阅。
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // 依次接收上游发出的值。
            flow.collect { event ->
                // 交给本次重组更新后的回调处理。
                currentOnEvent(event)
            }
        }
    }
}
