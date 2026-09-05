package com.lyf.composescaffold.feature.cart.presentation

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.model.Money
import com.lyf.composescaffold.core.model.formatMoney
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.data.repository.CartRepository
import com.lyf.composescaffold.core.model.article.Article
import com.lyf.composescaffold.core.model.article.ArticlePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repository: CartRepository): CartViewModel =
        CartViewModel(repository)

    @Test
    fun formatMoneyUsesMinorUnits() {
        assertThat(formatMoney(Money(0))).isEqualTo("¥0.00")
        assertThat(formatMoney(Money(5))).isEqualTo("¥0.05")
        assertThat(formatMoney(Money(3_380))).isEqualTo("¥33.80")
    }

    @Test
    fun initialLoadShowsFirstPage() = runTest(dispatcher) {
        val repository = FakeCartRepository { page -> successPage(page) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.dataList).hasSize(3)
        assertThat(state.isInitializing).isFalse()
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.error).isNull()
        assertThat(repository.requestedPages).containsExactly(1)
    }

    @Test
    fun initialFailureShowsErrorThenRetryRecovers() = runTest(dispatcher) {
        val repository = FakeCartRepository { page ->
            if (page == 1 && requestedPages.size == 1) {
                Result.failure(IllegalStateException("网络连接失败"))
            } else {
                successPage(page)
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo(CartError.LOAD_FAILED)
        assertThat(viewModel.uiState.value.dataList).isEmpty()

        viewModel.onIntent(CartIntent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.dataList).hasSize(3)
        assertThat(repository.requestedPages).containsExactly(1, 1).inOrder()
    }

    @Test
    fun positionBasedTotalsAndSelectAll() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeCartRepository { page -> successPage(page) })
        advanceUntilIdle()

        // 演示价 = position + 1（分）：选中第 0、1 条合计 1 + 2 = 3 分。
        viewModel.onIntent(CartIntent.ToggleItem(1))
        viewModel.onIntent(CartIntent.ToggleItem(2))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.total).isEqualTo(Money(3))
        assertThat(viewModel.uiState.value.allSelected).isFalse()

        viewModel.onIntent(CartIntent.ToggleSelectAll)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.allSelected).isTrue()
        assertThat(viewModel.uiState.value.selectedCount).isEqualTo(3)
        assertThat(viewModel.uiState.value.total).isEqualTo(Money(6))

        viewModel.onIntent(CartIntent.ToggleSelectAll)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.allSelected).isFalse()
        assertThat(viewModel.uiState.value.total).isEqualTo(Money.zero())
    }

    @Test
    fun refreshFailureKeepsItemsAndShowsError() = runTest(dispatcher) {
        val repository = FakeCartRepository { page ->
            if (requestedPages.size == 1) successPage(page)
            else Result.failure(IllegalStateException("网络连接失败"))
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CartIntent.Refresh)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo(CartError.LOAD_FAILED)
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
        assertThat(viewModel.uiState.value.dataList).hasSize(3)
    }

    @Test
    fun refreshSuccessClearsStaleError() = runTest(dispatcher) {
        val repository = FakeCartRepository { page ->
            if (requestedPages.size == 1) {
                Result.failure(IllegalStateException("网络连接失败"))
            } else {
                successPage(page)
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isEqualTo(CartError.LOAD_FAILED)

        viewModel.onIntent(CartIntent.Refresh)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isNull()
        assertThat(viewModel.uiState.value.dataList).hasSize(3)
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
    }

    @Test
    fun loadMoreAppendsNextPageThenStopsAtEnd() = runTest(dispatcher) {
        val repository = FakeCartRepository { page -> successPage(page) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CartIntent.LoadMore)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.dataList).hasSize(6)
        assertThat(viewModel.uiState.value.loadMoreState).isEqualTo(LoadMoreState.End)
        assertThat(repository.requestedPages).containsExactly(1, 2).inOrder()

        // 已到最后一页，继续触底短路，不再发请求。
        viewModel.onIntent(CartIntent.LoadMore)
        advanceUntilIdle()
        assertThat(repository.requestedPages).containsExactly(1, 2).inOrder()
    }

    @Test
    fun loadMoreDeduplicatesRepeatedIdsAcrossPages() = runTest(dispatcher) {
        val repository = FakeCartRepository { page ->
            if (page == 1) {
                Result.success(ArticlePage(items = articles(listOf(1L, 2L, 3L)), hasMore = true))
            } else {
                // 第 2 页重复返回第 1 页的 id=2，并夹带页内重复的 id=5。
                Result.success(ArticlePage(items = articles(listOf(2L, 5L, 5L, 6L)), hasMore = false))
            }
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(CartIntent.LoadMore)
        advanceUntilIdle()

        // 撞 id 条目被过滤，列表 id 唯一，LazyColumn 的 item key 不会重复。
        assertThat(viewModel.uiState.value.dataList.map { it.article.id })
            .containsExactly(1L, 2L, 3L, 5L, 6L)
            .inOrder()
        // 演示价按展示位置计：去重后 id=6 落在 position 4，定价 5 分。
        assertThat(viewModel.uiState.value.dataList.last().unitPrice).isEqualTo(Money(5))
    }

    /** 页码 p 返回 3 条数据，第 2 页起 hasMore=false。 */
    private fun successPage(page: Int): Result<ArticlePage> = Result.success(
        ArticlePage(
            items = articles((1..3).map { offset -> ((page - 1) * 3 + offset).toLong() }),
            hasMore = page < 2,
        ),
    )

    private fun articles(ids: List<Long>): List<Article> = ids.map { id ->
        Article(
            id = id,
            title = "文章 $id",
            author = "作者",
            chapterName = "体系课程",
            link = "https://www.wanandroid.com/article/$id",
            niceDate = "1 小时前",
        )
    }
}

private class FakeCartRepository(
    private val handler: FakeCartRepository.(page: Int) -> Result<ArticlePage>,
) : CartRepository {
    val requestedPages = mutableListOf<Int>()

    override suspend fun loadPage(page: Int): Result<ArticlePage> {
        requestedPages += page
        return handler(page)
    }
}
