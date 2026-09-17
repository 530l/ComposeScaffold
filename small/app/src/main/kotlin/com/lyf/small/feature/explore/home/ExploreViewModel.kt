package com.lyf.small.feature.explore.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.small.core.data.network.ApiCodes
import com.lyf.small.core.data.network.isConnectivityFailure
import com.lyf.small.data.content.model.Article
import com.lyf.small.data.content.model.ArticlePage
import com.lyf.small.data.content.repository.ContentRepository
import com.lyf.small.data.content.repository.apiErrorCode
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 探索页状态源。
 *
 * 全部可观察状态收敛在单一 [uiState] 中原子更新；一次性提示经 [events] 发送，不重放。
 * 页面通过 [refresh]、[retryInitial]、[loadMore] 直接驱动加载。
 */
@HiltViewModel
internal class ExploreViewModel @Inject constructor(
    private val repository: ContentRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = mutableUiState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<ExploreEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ExploreEvent> = mutableEvents.asSharedFlow()

    private var homeJob: Job? = null
    private var loadMoreJob: Job? = null
    private var nextPage = FIRST_LOAD_MORE_PAGE

    init {
        initialize()
    }

    /** 整页失败后的重试入口；加载中重复调用会被忽略。 */
    fun retryInitial() {
        if (mutableUiState.value.phase == ExploreUiState.Phase.Loading) return
        initialize()
    }

    /**
     * 下拉刷新：轮播与文章首页并发请求，全部返回后统一落地，
     * 再收起刷新动画并按 [toMessageEvent] 的优先级发送一次性提示。
     * 刷新期间的迟到取消由 await 传播，不会产生半落地状态。
     */
    fun refresh() {
        val state = mutableUiState.value
        if (state.isRefreshing || state.phase == ExploreUiState.Phase.Loading) return
        mutableUiState.update { it.copy(isRefreshing = true) }
        homeJob = viewModelScope.launch {
            // launch   干完就行，无结果    // async  干完交货，可 await
            // coroutineScope 等全部干完   // awaitAll 等全部交货
            // coroutineScope { launch A; launch B }          // 并发 → 等完 → 无结果
            // coroutineScope { async A; async B }.awaitAll() // 并发 → 等完 → 取结果

            // 1. coroutineScope 创建 ScopeCoroutine(Job)
            // 2. block 里的 launch/async 成为它的子 Job
            // 3. 父协程在 coroutineScope 处挂起
            // 4. 子 Job 完成时通知 ScopeCoroutine
            // 5. 所有子 Job 完成后，ScopeCoroutine 完成
            // 6. 恢复父协程，继续往下执行

            // 创建子 Job → 父协程挂起 → 子 Job 完成回调 → 无子 Job 则完成 → 恢复父协程。
            // 一个错全消，异常上抛；想隔离，用 supervisorScope。

            val (bannerRes, articleRes) = coroutineScope {
                val banners = async { repository.loadBanners() }
                val articles = async { repository.loadArticles(FIRST_ARTICLE_PAGE) }
                banners.await() to articles.await()
            }
            bannerRes.onSuccess { mutableUiState.update { it.copy(banners = data) } }
            articleRes.onSuccess { applyFirstPage(data) }
            val failures = listOfNotNull(
                bannerRes as? ApiResponse.Failure<*>,
                articleRes as? ApiResponse.Failure<*>,
            )
            mutableUiState.update { it.copy(isRefreshing = false) }
            failures.toMessageEvent()?.let(mutableEvents::tryEmit)
        }
    }

    /**
     * 加载下一页：按 id 去重后追加；整页重复时仅推进页码，
     * 由下一次触底自然翻过服务端页漂移产生的重复页。
     * 未满足 [canLoadMore] 或已到末页时静默忽略。
     */
    fun loadMore() {
        if (!canLoadMore()) return
        mutableUiState.update { it.copy(loadMoreState = ExploreLoadMoreState.Loading) }
        loadMoreJob = viewModelScope.launch {
            val response = repository.loadArticles(nextPage)
            currentCoroutineContext().ensureActive()
            when (response) {
                is ApiResponse.Failure<*> -> {
                    mutableUiState.update {
                        it.copy(
                            loadMoreState = ExploreLoadMoreState.Failed(
                                isOffline = response.isConnectivityFailure(),
                            ),
                        )
                    }
                }

                is ApiResponse.Success -> {
                    nextPage += 1
                    applyLoadedPage(response.data)
                }
            }
        }
    }

    /** 首次加载：重置除轮播外的全部状态并整页加载；进行中的加载与分页会被取消。 */
    private fun initialize() {
        homeJob?.cancel()
        loadMoreJob?.cancel()
        nextPage = FIRST_LOAD_MORE_PAGE
        mutableUiState.update { ExploreUiState(banners = it.banners) }
        homeJob = viewModelScope.launch {
            // 轮播不参与整页状态：独立加载，失败保留旧图。
            launch {
                repository.loadBanners().onSuccess {
                    mutableUiState.update { it.copy(banners = data) }
                }
            }
            val response = repository.loadArticles(FIRST_ARTICLE_PAGE)
            currentCoroutineContext().ensureActive()
            when (response) {
                is ApiResponse.Failure<*> -> mutableUiState.update { it.copy(phase = ExploreUiState.Phase.Error) }

                is ApiResponse.Success -> {
                    applyFirstPage(response.data)
                    mutableUiState.update { it.copy(phase = ExploreUiState.Phase.Idle) }
                }
            }
        }
    }

    /** 触底加载闸门：整页就绪、非刷新、已有内容、未到末页且当前无分页进行。 */
    private fun canLoadMore(): Boolean = with(mutableUiState.value) {
        phase == ExploreUiState.Phase.Idle &&
            !isRefreshing &&
            articles.isNotEmpty() &&
            loadMoreState != ExploreLoadMoreState.End &&
            loadMoreState !is ExploreLoadMoreState.Loading
    }

    /** 首页落地：替换文章并推导分页状态；刷新成功会解除整页错误态。 */
    private fun applyFirstPage(page: ArticlePage) {
        nextPage = FIRST_LOAD_MORE_PAGE
        mutableUiState.update {
            it.copy(
                articles = page.articles,
                loadMoreState = if (page.hasMore && page.articles.isNotEmpty()) {
                    ExploreLoadMoreState.Idle
                } else {
                    ExploreLoadMoreState.End
                },
                phase = if (it.phase == ExploreUiState.Phase.Error) {
                    ExploreUiState.Phase.Idle
                } else {
                    it.phase
                },
            )
        }
    }

    /** 追加页落地：与已有文章按 id 去重后追加，并依据服务端分页信息推进末页状态。 */
    private fun applyLoadedPage(page: ArticlePage) {
        val existingIds = mutableUiState.value.articles.mapTo(mutableSetOf(), Article::id)
        val newArticles = page.articles.distinctBy(Article::id).filterNot { it.id in existingIds }
        mutableUiState.update {
            it.copy(
                articles = it.articles + newArticles,
                loadMoreState = if (page.hasMore && page.articles.isNotEmpty()) {
                    ExploreLoadMoreState.Idle
                } else {
                    ExploreLoadMoreState.End
                },
            )
        }
    }

    /** 失败到提示的优先级：登录失效 > 网络不可用 > 通用失败。 */
    private fun List<ApiResponse.Failure<*>>.toMessageEvent(): ExploreEvent? = when {
        isEmpty() -> null
        any { it.apiErrorCode() == ApiCodes.TOKEN_EXPIRED } -> ExploreEvent.RequireLogin
        any { it.isConnectivityFailure() } -> ExploreEvent.RefreshOffline
        else -> ExploreEvent.RefreshFailed
    }

    private companion object {
        const val FIRST_ARTICLE_PAGE = 0
        const val FIRST_LOAD_MORE_PAGE = 1
    }
}
