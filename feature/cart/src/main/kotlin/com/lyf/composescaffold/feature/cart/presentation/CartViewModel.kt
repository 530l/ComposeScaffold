package com.lyf.composescaffold.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.composescaffold.core.common.log.AppLogger
import com.lyf.composescaffold.core.model.Money
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableController
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableUiState
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.design.ui.loadmore.Page
import com.lyf.composescaffold.core.data.repository.CartRepository
import com.lyf.composescaffold.core.model.article.Article
import com.lyf.composescaffold.core.model.article.ArticlePage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 演示价规则：列表第 position 条（0 基）定价 (position + 1) 分，
 * 底栏合计为已选条目的位置累加。仅用于脚手架演示金额链路，无业务含义。
 */
internal fun demoUnitPrice(position: Int): Money = Money((position + 1).toLong())

internal data class CartItemUiState(
    val article: Article,
    val unitPrice: Money,
    val selected: Boolean = false,
)

internal data class CartUiState(
    override val dataList: List<CartItemUiState> = emptyList(),
    override val isRefreshing: Boolean = false,
    override val isInitializing: Boolean = true,
    override val loadMoreState: LoadMoreState = LoadMoreState.Idle,
    val error: CartError? = null,
) : LoadableUiState<CartItemUiState, CartUiState> {
    val allSelected: Boolean get() = dataList.isNotEmpty() && dataList.all(CartItemUiState::selected)
    val selectedCount: Int get() = dataList.count(CartItemUiState::selected)
    val total: Money
        get() = dataList
            .filter(CartItemUiState::selected)
            .fold(Money.zero()) { acc, item -> acc + item.unitPrice }

    override fun copyState(
        dataList: List<CartItemUiState>,
        isRefreshing: Boolean,
        isInitializing: Boolean,
        loadMoreState: LoadMoreState,
    ): CartUiState = copy(
        dataList = dataList,
        isRefreshing = isRefreshing,
        isInitializing = isInitializing,
        loadMoreState = loadMoreState,
    )
}

internal enum class CartError {
    LOAD_FAILED,
}

internal sealed interface CartIntent {
    data class ToggleItem(val itemId: Long) : CartIntent
    data object ToggleSelectAll : CartIntent
    data object Refresh : CartIntent
    data object LoadMore : CartIntent
    data object Retry : CartIntent
    data object Checkout : CartIntent
}

internal sealed interface CartEvent {
    data class Checkout(val selectedItemIds: List<Long>) : CartEvent
}

@HiltViewModel
internal class CartViewModel @Inject constructor(
    private val repository: CartRepository,
) : ViewModel() {
    private val loadable: LoadableController<CartItemUiState, CartUiState> =
        LoadableController(
            scope = viewModelScope,
            initialUiState = CartUiState(),
            loadPage = ::loadPage,
            onError = ::onLoadError,
        )

    val uiState: StateFlow<CartUiState> = loadable.uiState
    private val eventChannel = Channel<CartEvent>(capacity = Channel.BUFFERED)
    val events: Flow<CartEvent> = eventChannel.receiveAsFlow()

    init {
        loadable.initialize()
    }

    /** 加载成功即清除旧错误（banner 随下一次成功消失）。 */
    private suspend fun loadPage(page: Int): Result<Page<CartItemUiState>> {
        val currentList = loadable.uiState.value.dataList
        val startPosition = if (page == LoadableController.FIRST_PAGE) 0 else currentList.size
        // wanandroid 相邻页可能返回重复 id：先页内去重；加载更多再滤掉与已有列表撞 id 的条目，
        // 否则 LazyColumn 的 item key 重复会直接崩溃。刷新是整页替换，不与旧列表比对。
        val existingIds = if (page == LoadableController.FIRST_PAGE) {
            emptySet()
        } else {
            currentList.mapTo(mutableSetOf()) { it.article.id }
        }
        return repository.loadPage(page).map { result: ArticlePage ->
            Page(
                items = result.items
                    .distinctBy { it.id }
                    .filterNot { it.id in existingIds }
                    .mapIndexed { index, article ->
                        CartItemUiState(
                            article = article,
                            unitPrice = demoUnitPrice(startPosition + index),
                        )
                    },
                hasMore = result.hasMore,
                sourceItemCount = result.items.size,
            )
        }.onSuccess {
            loadable.updateState { state -> state.copy(error = null) }
        }
    }

    private fun onLoadError(error: Throwable, @Suppress("UNUSED_PARAMETER") isListEmpty: Boolean) {
        AppLogger.error(TAG, error) { "文章列表加载失败" }
        loadable.updateState { state -> state.copy(error = CartError.LOAD_FAILED) }
    }

    fun onIntent(intent: CartIntent) {
        when (intent) {
            is CartIntent.ToggleItem -> loadable.updateState { state ->
                state.copy(
                    dataList = state.dataList.map {
                        if (it.article.id == intent.itemId) it.copy(selected = !it.selected) else it
                    },
                )
            }

            CartIntent.ToggleSelectAll -> loadable.updateState { state ->
                val target = !state.allSelected
                state.copy(dataList = state.dataList.map { it.copy(selected = target) })
            }

            CartIntent.Refresh -> loadable.refresh()
            CartIntent.LoadMore -> loadable.loadMore()
            CartIntent.Retry -> loadable.initialize()
            CartIntent.Checkout ->
                uiState.value.dataList
                    .filter(CartItemUiState::selected)
                    .map { item -> item.article.id }
                    .takeIf(List<Long>::isNotEmpty)
                    ?.let { selectedIds ->
                        eventChannel.trySend(CartEvent.Checkout(selectedIds))
                    }
        }
    }

    private companion object {
        const val TAG = "CartViewModel"
    }
}
