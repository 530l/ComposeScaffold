package com.lyf.small.feature.explore.home

import androidx.lifecycle.viewModelScope
import com.lyf.small.core.common.log.AppLogger
import com.lyf.small.core.data.network.NetworkError
import com.lyf.small.core.data.network.NetworkException
import com.lyf.small.core.presentation.StateViewModel
import com.lyf.small.data.content.model.Article
import com.lyf.small.data.content.model.ArticlePage
import com.lyf.small.data.content.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** 探索页状态与请求协调入口。 */
@HiltViewModel
internal class ExploreViewModel @Inject constructor(
    private val repository: ContentRepository,
) : StateViewModel<ExploreUiState, ExploreIntent, ExploreEvent>(
    initialState = ExploreUiState(),
) {
    private var homeJob: Job? = null
    private var loadMoreJob: Job? = null
    private var nextArticlePage = FIRST_LOAD_MORE_PAGE
    private var hasMoreArticles = true

    init {
        initialize()
    }

    override fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.Refresh -> refresh()
            ExploreIntent.RetryInitial -> initialize()
            ExploreIntent.LoadMore -> loadMore(isRetry = false)
            ExploreIntent.RetryLoadMore -> loadMore(isRetry = true)
        }
    }

    /** 首次请求并发执行；文章先返回即可结束整页 Loading，轮播失败不阻断正文。 */
    private fun initialize() {
        homeJob?.cancel()
        loadMoreJob?.cancel()
        nextArticlePage = FIRST_LOAD_MORE_PAGE
        hasMoreArticles = true
        updateState {
            copy(
                articles = emptyList(),
                isInitializing = true,
                hasInitialError = false,
                isRefreshing = false,
                loadMoreState = ExploreLoadMoreState.Idle,
            )
        }
        homeJob = viewModelScope.launch {
            coroutineScope {
                val bannerJob = launch {
                    runRequest("首页轮播加载失败") { repository.loadBanners() }
                        .onSuccess { banners -> updateState { copy(banners = banners) } }
                }
                val articleJob = launch {
                    applyInitialArticles(runRequest("首页文章加载失败") { repository.loadArticles(FIRST_ARTICLE_PAGE) })
                }
                joinAll(bannerJob, articleJob)
            }
        }
    }

    /** 刷新取消分页，并在轮播与文章都完成后结束刷新动画。 */
    private fun refresh() {
        val state = currentState
        if (state.isInitializing || state.isRefreshing || homeJob?.isActive == true) return

        loadMoreJob?.cancel()
        val previousLoadMoreState = when (state.loadMoreState) {
            ExploreLoadMoreState.Loading -> ExploreLoadMoreState.Idle
            else -> state.loadMoreState
        }
        updateState {
            copy(
                isRefreshing = true,
                loadMoreState = ExploreLoadMoreState.Idle,
            )
        }

        homeJob = viewModelScope.launch {
            val failures = coroutineScope {
                listOf(
                    async { refreshBanners() },
                    async { refreshArticles(previousLoadMoreState) },
                ).awaitAll().filterNotNull()
            }
            currentCoroutineContext().ensureActive()
            updateState { copy(isRefreshing = false) }
            failures.toRefreshEvent()?.let(::emitEvent)
        }
    }

    private suspend fun refreshBanners(): Throwable? =
        runRequest("首页轮播刷新失败") { repository.loadBanners() }
            .fold(
                onSuccess = { banners ->
                    updateState { copy(banners = banners) }
                    null
                },
                onFailure = { error -> error },
            )

    private suspend fun refreshArticles(
        previousLoadMoreState: ExploreLoadMoreState,
    ): Throwable? = runRequest("首页文章刷新失败") { repository.loadArticles(FIRST_ARTICLE_PAGE) }
        .fold(
            onSuccess = { page ->
                applyFirstPage(page)
                null
            },
            onFailure = { error ->
                updateState {
                    copy(
                        hasInitialError = articles.isEmpty(),
                        loadMoreState = previousLoadMoreState,
                    )
                }
                error
            },
        )

    private fun applyInitialArticles(result: Result<ArticlePage>) {
        result.fold(
            onSuccess = { page -> applyFirstPage(page) },
            onFailure = {
                updateState {
                    copy(
                        isInitializing = false,
                        hasInitialError = true,
                        loadMoreState = ExploreLoadMoreState.Idle,
                    )
                }
            },
        )
    }

    private fun applyFirstPage(page: ArticlePage) {
        nextArticlePage = FIRST_LOAD_MORE_PAGE
        hasMoreArticles = page.hasMore && page.articles.isNotEmpty()
        updateState {
            copy(
                articles = page.articles,
                isInitializing = false,
                hasInitialError = false,
                loadMoreState = if (hasMoreArticles) {
                    ExploreLoadMoreState.Idle
                } else {
                    ExploreLoadMoreState.End
                },
            )
        }
    }

    /** 一次最多跨过三个全重复页，重复数据不会被误判为末页。 */
    private fun loadMore(isRetry: Boolean) {
        if (!canLoadMore()) return
        updateState { copy(loadMoreState = ExploreLoadMoreState.Loading) }
        loadMoreJob = viewModelScope.launch {
            loadMorePages(isRetry)
        }
    }

    private fun canLoadMore(): Boolean = with(currentState) {
        !isInitializing && !isRefreshing && articles.isNotEmpty() &&
            hasMoreArticles && homeJob?.isActive != true && loadMoreJob?.isActive != true
    }

    private suspend fun loadMorePages(isRetry: Boolean) {
        val requestStartedAt = TimeSource.Monotonic.markNow()
        var pageToLoad = nextArticlePage
        repeat(MAX_DUPLICATE_PAGES_PER_REQUEST) {
            val result = runRequest("文章分页加载失败") { repository.loadArticles(pageToLoad) }
            currentCoroutineContext().ensureActive()
            result.exceptionOrNull()?.let { error ->
                handleLoadMoreFailure(
                    error = error,
                    failedPage = pageToLoad,
                    isRetry = isRetry,
                    requestStartedAt = requestStartedAt,
                )
                return
            }
            pageToLoad += 1
            nextArticlePage = pageToLoad
            if (applyLoadedPage(result.getOrThrow())) return
        }

        nextArticlePage = pageToLoad
        updateState {
            copy(loadMoreState = ExploreLoadMoreState.Failed(isOffline = false))
        }
        AppLogger.warning(TAG) { "文章分页连续三页没有新增内容" }
    }

    private fun applyLoadedPage(page: ArticlePage): Boolean {
        val existingIds = currentState.articles.mapTo(mutableSetOf(), Article::id)
        val newArticles = page.articles.distinctBy(Article::id).filterNot { it.id in existingIds }
        val reachedEnd = !page.hasMore || page.articles.isEmpty()
        if (newArticles.isEmpty() && !reachedEnd) return false
        hasMoreArticles = !reachedEnd
        updateState {
            copy(
                articles = articles + newArticles,
                loadMoreState = if (hasMoreArticles) {
                    ExploreLoadMoreState.Idle
                } else {
                    ExploreLoadMoreState.End
                },
            )
        }
        return true
    }

    private suspend fun handleLoadMoreFailure(
        error: Throwable, failedPage: Int,
        isRetry: Boolean, requestStartedAt: TimeMark,
    ) {
        if (isRetry) requestStartedAt.awaitMinimumRetryLoading()
        nextArticlePage = failedPage
        updateState {
            copy(
                loadMoreState = ExploreLoadMoreState.Failed(
                    isOffline = error.isConnectivityFailure(),
                ),
            )
        }
    }

    /** 断网会立即失败，手动重试时保留短暂加载反馈，避免失败 Footer 闪烁。 */
    private suspend fun TimeMark.awaitMinimumRetryLoading() {
        val remainingDuration = MINIMUM_RETRY_LOADING_DURATION - elapsedNow()
        if (remainingDuration.isPositive()) delay(remainingDuration)
    }

    private fun List<Throwable>.toRefreshEvent(): ExploreEvent? = when {
        isEmpty() -> null
        any { it.isConnectivityFailure() } -> ExploreEvent.RefreshOffline
        else -> ExploreEvent.RefreshFailed
    }

    private fun Throwable.isConnectivityFailure(): Boolean =
        (this as? NetworkException)?.error is NetworkError.Connectivity

    private companion object {
        const val TAG = "ExploreViewModel"
        const val FIRST_ARTICLE_PAGE = 0
        const val FIRST_LOAD_MORE_PAGE = 1
        const val MAX_DUPLICATE_PAGES_PER_REQUEST = 3
        val MINIMUM_RETRY_LOADING_DURATION = 800.milliseconds
    }
}
