package com.lyf.small.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.small.core.common.log.AppLogger
import com.lyf.small.core.data.network.safeLogSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    /**
     * 页面级请求统一入口（发起即忘）：在 viewModelScope 中执行挂起 lambda，
     * 成功/失败分别回调，失败自动记录脱敏日志。核心执行逻辑见 [runRequest]。
     *
     * @param operation 失败日志用的操作描述，如 "首页轮播加载失败"。
     * @return 对应的 Job，调用方可用于取消，或在并发场景等待完成。
     */
    protected fun <T : Any> request(
        operation: String,
        onSuccess: (T) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
        block: suspend () -> Result<T>,
    ): Job = viewModelScope.launch {
        runRequest(operation, block).fold(onSuccess, onFailure)
    }

    /**
     * 请求执行核心：block 正常返回的 Result 与 block 自身抛出的异常归一到同一失败通道；
     * 协程取消不视为业务失败，失败自动记录脱敏日志。
     *
     * 需要嵌入调用方协程结构（coroutineScope/async 并发编排等）时使用，
     * 保证取消沿父子协程传播；发起即忘的顶层请求直接用 [request]。
     */
    protected suspend fun <T : Any> runRequest(
        operation: String,
        block: suspend () -> Result<T>,
    ): Result<T> {
        val result = try {
            block()
        } catch (error: CancellationException) {
            // 取消不是业务失败，继续向上传播，保证结构化并发的取消语义不被吞掉。
            throw error
        } catch (error: Throwable) {
            // block 自身抛出的异常归一到与 Repository 相同的失败通道。
            Result.failure(error)
        }
        result.onFailure { error ->
            AppLogger.warning(requestTag) { "$operation: ${error.safeLogSummary()}" }
        }
        return result
    }

    /** 日志 tag 取实际子类名，基类无需感知调用方。 */
    private val requestTag: String
        get() = this::class.simpleName ?: "StateViewModel"

    private companion object {
        const val DEFAULT_EVENT_BUFFER_CAPACITY = 1
    }
}
