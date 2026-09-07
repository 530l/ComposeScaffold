package com.lyf.composescaffold.core.design.ui.loadmore

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 分页列表的 UiState 契约。feature 的 UiState data class 实现本接口后，
 * 刷新/分页状态就嵌在自己的不可变状态里，由 [LoadableController] 统一驱动，
 * feature 仍可在同一个 UiState 上扩展业务字段。
 */
interface LoadableUiState<T, S : LoadableUiState<T, S>> {
    val dataList: List<T>
    val isRefreshing: Boolean
    val isInitializing: Boolean
    val loadMoreState: LoadMoreState

    fun copyState(
        dataList: List<T> = this.dataList,
        isRefreshing: Boolean = this.isRefreshing,
        isInitializing: Boolean = this.isInitializing,
        loadMoreState: LoadMoreState = this.loadMoreState,
    ): S
}

/**
 * 分页列表状态机：封装初始化、下拉刷新、触底加载的互斥与去重规则。
 *
 * 互斥规则（UI 触发层无需自行去重，可任意连发）：
 * - 首页加载进行中（初始化、刷新或静默刷新，含本地数据已先行展示的窗口）：加载更多被忽略；
 * - 刷新进行中（含不亮指示器的 silent 刷新）：重复刷新被忽略；
 * - 加载更多进行中：重复触发被忽略；
 * - 新刷新会取消进行中的加载更多，成功后再提交新页码与结束标记；
 * - 到达最后一页（[LoadMoreState.End]）后加载更多短路，刷新成功时重置。
 *
 * 防御规则：服务端原始空页判定结束；过滤后无新增项时最多连续读取三页，随后进入可重试失败。
 * 所有入口由同一 UI 线程调用；请求的 IO 调度由 loader 负责。
 *
 * [onError] 负责错误分层：`isListEmpty` 为 true 时适合整页错误占位，
 * 否则适合非阻断提示（banner/snackbar）。展示文案由 feature 的资源提供。
 */
class LoadableController<T, S : LoadableUiState<T, S>>(
    private val scope: CoroutineScope,
    initialUiState: S,
    private val loadPage: suspend (page: Int) -> Result<Page<T>>,
    private val localData: (suspend () -> List<T>)? = null,
    private val onError: ((error: Throwable, isListEmpty: Boolean) -> Unit)? = null,
) {
    private val _uiState = MutableStateFlow(initialUiState)

    /** 与 ViewModel 自己的 StateFlow 一样暴露只读流，item 级更新走 [updateState]。 */
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private var initializeJob: Job? = null
    private var refreshJob: Job? = null
    private var loadMoreJob: Job? = null

    /** 已到最后一页；与 UiState 的 [LoadMoreState.End] 同步，刷新成功时重置。 */
    private var reachEnd = false

    /** 下一次加载更多请求的页码。 */
    private var nextPage = FIRST_PAGE

    /** 供 item 级更新（选中、局部刷新等）使用，不暴露可变流。 */
    fun updateState(transform: (S) -> S) {
        _uiState.update(transform)
    }

    /**
     * 首次进入页面调用一次。提供 [localData] 时先展示本地数据再拉服务端
     * （本地数据非空则立即结束 initializing），服务端结果到达后整页替换。
     */
    fun initialize() {
        initializeJob?.cancel()
        refreshJob?.cancel()
        loadMoreJob?.cancel()
        reachEnd = false
        nextPage = FIRST_PAGE + 1
        _uiState.update {
            it.copyState(
                dataList = emptyList(),
                isInitializing = true,
                isRefreshing = false,
                loadMoreState = LoadMoreState.Idle,
            )
        }
        initializeJob = scope.launch {
            localData?.let { local ->
                // 本地数据只是过渡展示，失败时静默降级为无本地数据，远端结果才是最终状态；
                // 协程取消必须原样上抛。
                val items = try {
                    local().also { currentCoroutineContext().ensureActive() }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    emptyList()
                }
                if (items.isNotEmpty()) {
                    _uiState.update {
                        it.copyState(dataList = items, isInitializing = false)
                    }
                }
            }
            loadPageSafely(FIRST_PAGE).fold(
                onSuccess = ::applyRefreshPage,
                onFailure = ::applyLoadFailure,
            )
        }
    }

    /** 下拉刷新；`silent = true` 时不亮刷新指示器（预取、tab 切换等场景）。 */
    fun refresh(silent: Boolean = false) {
        // silent 刷新不亮指示器，须以 job 活跃状态互斥，防止并发首页请求。
        if (_uiState.value.isRefreshing || refreshJob?.isActive == true) return
        initializeJob?.cancel()
        loadMoreJob?.cancel()
        val previousLoadMoreState = _uiState.value.loadMoreState.let { state ->
            if (state == LoadMoreState.Loading) LoadMoreState.Idle else state
        }
        _uiState.update {
            it.copyState(
                isRefreshing = !silent,
                isInitializing = false,
                loadMoreState = LoadMoreState.Idle,
            )
        }
        refreshJob = scope.launch {
            loadPageSafely(FIRST_PAGE).fold(
                onSuccess = ::applyRefreshPage,
                onFailure = { error ->
                    // 刷新失败时列表、页码、末页标记仍属于同一批数据。
                    _uiState.update { it.copyState(loadMoreState = previousLoadMoreState) }
                    applyLoadFailure(error)
                },
            )
        }
    }

    /** 触底加载/失败重试共用入口，去重规则见类注释。 */
    fun loadMore() {
        val state = _uiState.value
        if (reachEnd || state.isRefreshing || state.isInitializing) return
        // silent 刷新不亮指示器、本地数据先行展示会提前结束 initializing，
        // 这两个窗口的首页请求在途时同样禁止触底加载。
        if (initializeJob?.isActive == true || refreshJob?.isActive == true) return
        if (state.loadMoreState is LoadMoreState.Loading) return
        if (state.dataList.isEmpty()) return
        loadMoreJob?.cancel()
        _uiState.update { it.copyState(loadMoreState = LoadMoreState.Loading) }
        loadMoreJob = scope.launch {
            loadNextPageWithNewItems().fold(
                onSuccess = { page ->
                    reachEnd = !page.hasMore || page.sourceItemCount == 0
                    nextPage += 1
                    _uiState.update {
                        it.copyState(
                            dataList = it.dataList + page.items,
                            loadMoreState = if (reachEnd) LoadMoreState.End else LoadMoreState.Idle,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copyState(loadMoreState = LoadMoreState.Failed) }
                    onError?.invoke(error, false)
                },
            )
        }
    }

    private fun applyRefreshPage(page: Page<T>) {
        reachEnd = !page.hasMore || page.sourceItemCount == 0
        nextPage = FIRST_PAGE + 1
        _uiState.update {
            it.copyState(
                dataList = page.items,
                isRefreshing = false,
                isInitializing = false,
                loadMoreState = if (reachEnd) LoadMoreState.End else LoadMoreState.Idle,
            )
        }
    }

    private fun applyLoadFailure(error: Throwable) {
        val isListEmpty = _uiState.value.dataList.isEmpty()
        _uiState.update {
            it.copyState(isRefreshing = false, isInitializing = false)
        }
        onError?.invoke(error, isListEmpty)
    }

    /** 全重复页消耗了服务端页码但不结束列表；达到上限后允许从下一未读页手动重试。 */
    private suspend fun loadNextPageWithNewItems(): Result<Page<T>> {
        repeat(MAX_PAGES_WITHOUT_NEW_ITEMS) {
            val result = loadPageSafely(nextPage)
            val page = result.getOrElse { return result }
            if (page.items.isNotEmpty() || !page.hasMore || page.sourceItemCount == 0) return result
            nextPage += 1
        }
        return Result.failure(IllegalStateException("连续分页没有新增数据，请手动重试"))
    }

    /** 协程取消原样上抛；loader 意外抛出的异常收敛为 Result.failure。 */
    private suspend fun loadPageSafely(page: Int): Result<Page<T>> = try {
        val result = loadPage(page)
        currentCoroutineContext().ensureActive()
        val error = result.exceptionOrNull()
        if (error is CancellationException) throw error
        result
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        /** 页码从 1 开始，feature 的 loader 闭包按同一约定映射到后端参数。 */
        const val FIRST_PAGE = 1
        private const val MAX_PAGES_WITHOUT_NEW_ITEMS = 3
    }
}
