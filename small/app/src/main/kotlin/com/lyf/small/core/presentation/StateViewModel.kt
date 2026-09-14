package com.lyf.small.core.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 业务页面统一的单向状态容器。
 *
 * 子类只处理 Intent 和业务编排；可观察状态与一次性事件始终以只读 Flow 暴露。
 * 静态页面不需要为了套用此基类而创建 ViewModel。
 */
abstract class StateViewModel<UiState : Any, Intent : Any, Event : Any>(
    initialState: UiState,
    eventBufferCapacity: Int = DEFAULT_EVENT_BUFFER_CAPACITY,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<Event>(
        extraBufferCapacity = eventBufferCapacity,
    )
    val events: Flow<Event> = mutableEvents.asSharedFlow()

    protected val currentState: UiState
        get() = mutableUiState.value

    abstract fun onIntent(intent: Intent)

    /** 基于当前不可变状态生成下一状态。 */
    protected fun updateState(transform: UiState.() -> UiState) {
        mutableUiState.update { state -> state.transform() }
    }

    /**
     * 一次性事件采用非阻塞发送且不重放，适合提示、导航等瞬时效果。
     * 必须在返回前台后继续成立的信息应放入 UiState，不能依赖此事件流。
     */
    protected fun emitEvent(event: Event): Boolean = mutableEvents.tryEmit(event)

    private companion object {
        const val DEFAULT_EVENT_BUFFER_CAPACITY = 1
    }
}
