package com.lyf.composescaffold.feature.browse.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.data.repository.FeedInteractionStore
import com.lyf.composescaffold.core.data.repository.FeedRepository
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableController
import com.lyf.composescaffold.core.design.ui.loadmore.Page
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** 列表、分页与模式路由；互动与大厅音乐分别交给协作类，ViewModel 不膨胀。 */
@HiltViewModel
internal class BrowseViewModel @Inject constructor(
    private val repository: FeedRepository,
    interactionStore: FeedInteractionStore,
    private val savedState: SavedStateHandle, // 保存用户上次选择的浏览模式。
    musicController: MusicPlayerController? = null, // 大厅播放交给全局音乐；为空表示该构建未接入。
) : ViewModel() {
    // 互动缓存、持久化与失败提示的协作入口。
    private val interactionCoordinator = FeedInteractionCoordinator(interactionStore, viewModelScope)
    val interactions = interactionCoordinator.interactions
    val interactionError = interactionCoordinator.error
    val interactionPending = interactionCoordinator.pending

    // 大厅对全局音乐的投影与播放入口。
    private val musicQueue = FeedMusicQueue(musicController)
    val musicPlaybackState = musicQueue.playbackState

    // 恢复上次保存的模式，无有效保存值时默认进入混合流。
    private val _mode = MutableStateFlow(
        FeedMode.entries.firstOrNull { it.name == savedState.get<String>("feed.mode") }
            ?: FeedMode.MIXED,
    )
    val mode = _mode.asStateFlow()

    private val initialized = mutableSetOf<FeedMode>()

    // 每种模式各持一套列表与分页状态机，切换模式不丢其他模式数据。
    private val controllers: Map<FeedMode, LoadableController<FeedItem, FeedUiState>> =
        FeedMode.entries.associateWith { selected ->
            // 复用通用分页状态机，不在页面复制分页逻辑。
            LoadableController<FeedItem, FeedUiState>(
                scope = viewModelScope,
                initialUiState = FeedUiState(),
                loadPage = { page -> load(selected, page) },
                // 失败时保留已加载列表并标记 failed，重试方式由 UI 判断。
                onError = { _, _ -> controller(selected).updateState { it.copy(failed = true) } },
            )
        }

    fun stateFor(mode: FeedMode) = controller(mode).uiState

    init {
        // 只初始化当前（默认或恢复出的）模式，其余模式首次进入时再加载。
        initialize(mode.value)
    }

    private fun controller(mode: FeedMode): LoadableController<FeedItem, FeedUiState> =
        controllers.getValue(mode)

    /** 懒初始化指定模式（已初始化过则忽略）。 */
    private fun initialize(mode: FeedMode) {
        if (!initialized.add(mode)) return
        controller(mode).initialize()
    }

    /** 读取一页作品，补齐互动状态并去重。 */
    private suspend fun load(mode: FeedMode, page: Int): Result<Page<FeedItem>> =
        repository.loadPage(mode, page, PAGE_SIZE).map { result ->
            // 与用户点击的互动更新互斥，避免旧值覆盖新点击。
            interactionCoordinator.prime(result.items)
            // 追加页与已加载列表去重；刷新首页不比较，由控制器整页替换旧列表。
            val existing =
                if (page == 1) emptySet() else controller(mode).uiState.value.dataList.mapTo(
                    mutableSetOf(),
                ) { it.key }
            controller(mode).updateState { it.copy(source = result.source, failed = false) }
            // hasMore 与条数交给通用分页控制器决定后续页码。
            Page(
                // 去掉页内重复和已经显示过的作品。
                result.items.distinctBy { it.key }.filterNot { it.key in existing },
                result.hasMore,
                // 条数用去重前统计，避免重复页被控制器误判为空末页。
                result.items.size,
            )
        }

    /** 用户交互意图统一分发入口。 */
    fun onIntent(intent: FeedIntent) {
        when (intent) {
            // 只切换当前选择并持久化，不丢弃其他模式已加载的列表。
            is FeedIntent.SelectMode -> {
                _mode.value = intent.mode
                savedState["feed.mode"] = intent.mode.name
                // 首次进入该模式才发起初始加载。
                initialize(intent.mode)
            }

            is FeedIntent.LoadMore -> controller(intent.mode).loadMore()
            is FeedIntent.Refresh -> controller(intent.mode).refresh()
            is FeedIntent.Retry -> {
                val controller = controller(intent.mode)
                // 空列表重试首次加载，有数据则重试追加。
                if (controller.uiState.value.dataList.isEmpty()) controller.initialize() else controller.loadMore()
            }

            is FeedIntent.ToggleLike -> interactionCoordinator.toggle(intent.key, like = true)
            is FeedIntent.ToggleSave -> interactionCoordinator.toggle(intent.key, like = false)
            is FeedIntent.PlayMusicTrack -> musicQueue.play(intent.track, intent.queue)
            is FeedIntent.ReadInteraction -> interactionCoordinator.read(intent.key)
            FeedIntent.RetryInteraction -> interactionCoordinator.retry()
            FeedIntent.DismissInteractionError -> interactionCoordinator.dismissError()
        }
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
