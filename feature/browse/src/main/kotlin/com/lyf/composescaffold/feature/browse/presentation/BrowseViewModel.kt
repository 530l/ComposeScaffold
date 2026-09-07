package com.lyf.composescaffold.feature.browse.presentation

import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.music.MusicTrack
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.data.repository.FeedInteractionStore
import com.lyf.composescaffold.core.data.repository.FeedRepository
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableController
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableUiState
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.design.ui.loadmore.Page
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Feed 信息流界面的只读 UI 状态聚合。 */
internal data class FeedUiState(
    override val dataList: List<FeedItem> = emptyList(),
    override val isRefreshing: Boolean = false,
    override val isInitializing: Boolean = true,
    override val loadMoreState: LoadMoreState = LoadMoreState.Idle,
    val source: FeedSource = FeedSource.MUSEAI_PUBLIC,
    val failed: Boolean = false,
    val revision: Int = 0,
) : LoadableUiState<FeedItem, FeedUiState> {
    /** 状态机状态深拷贝辅助方法，在下拉刷新完成且列表变更时自增版本号 */
    override fun copyState(
        dataList: List<FeedItem>,
        isRefreshing: Boolean,
        isInitializing: Boolean,
        loadMoreState: LoadMoreState,
    ): FeedUiState = copy(
        dataList = dataList,
        isRefreshing = isRefreshing,
        isInitializing = isInitializing,
        loadMoreState = loadMoreState,
        revision = if (this.isRefreshing && !isRefreshing && dataList !== this.dataList) revision + 1 else revision,
    )
}

/** Feed 页面用户操作意图（MVI Intent 单一入口）。 */
internal sealed interface FeedIntent {
    /** 切换视听模式（短视频/音乐/混合） */
    data class SelectMode(val mode: FeedMode) : FeedIntent
    /** 加载下一页 */
    data class LoadMore(val mode: FeedMode) : FeedIntent
    /** 下拉刷新 */
    data class Refresh(val mode: FeedMode) : FeedIntent
    /** 失败重试 */
    data class Retry(val mode: FeedMode) : FeedIntent
    /** 点赞/取消点赞切换 */
    data class ToggleLike(val key: String) : FeedIntent
    /** 收藏/取消收藏切换 */
    data class ToggleSave(val key: String) : FeedIntent
    /** 播放单首歌曲并更新播单队列 */
    data class PlayMusicTrack(
        val track: MusicTrack,
        val queue: List<MusicTrack>,
    ) : FeedIntent
    /** 关闭交互错误提示 */
    data object DismissInteractionError : FeedIntent
}

internal data class MusicHallPlayback(val trackId: String? = null, val isPlaying: Boolean = false)

/** 列表、分页和互动状态；不承载播放器或高频进度。 */
@HiltViewModel
internal class BrowseViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val interactionStore: FeedInteractionStore,
    private val savedState: SavedStateHandle,
    private val musicController: MusicPlayerController? = null,
) : ViewModel() {
    val musicPlaybackState = musicController?.state?.map {
        MusicHallPlayback(it.currentTrack?.id, it.isPlaying)
    }?.distinctUntilChanged()
    private val _mode = MutableStateFlow(
        FeedMode.entries.firstOrNull { it.name == savedState.get<String>("feed.mode") } ?: FeedMode.MIXED,
    )
    val mode = _mode.asStateFlow()
    private val _interactions = MutableStateFlow<Map<String, FeedInteraction>>(emptyMap())
    val interactions = _interactions.asStateFlow()
    private val _interactionError = MutableStateFlow(false)
    val interactionError = _interactionError.asStateFlow()
    private val interactionMutex = Mutex()
    private val initialized = mutableSetOf<FeedMode>()
    private val controllers: Map<FeedMode, LoadableController<FeedItem, FeedUiState>> = FeedMode.entries.associateWith { selected ->
        LoadableController<FeedItem, FeedUiState>(
            scope = viewModelScope,
            initialUiState = FeedUiState(),
            loadPage = { page -> load(selected, page) },
            onError = { _, _ -> controller(selected).updateState { it.copy(failed = true) } },
        )
    }

    /** 获取指定模式的 UI 状态流 */
    fun stateFor(mode: FeedMode) = controller(mode).uiState

    init {
        initialize(mode.value)
    }

    /** 获取指定模式对应的分页控制器 */
    private fun controller(mode: FeedMode): LoadableController<FeedItem, FeedUiState> = controllers.getValue(mode)

    /** 懒初始化指定模式（已初始化过则忽略） */
    private fun initialize(mode: FeedMode) {
        if (!initialized.add(mode)) return
        controller(mode).initialize()
    }

    /** 核心分页加载业务。 */
    private suspend fun load(mode: FeedMode, page: Int): Result<Page<FeedItem>> =
        repository.loadPage(mode, page, PAGE_SIZE).map { result ->
            interactionMutex.withLock {
                result.items.forEach { item ->
                    if (item.key !in _interactions.value) {
                        val interaction = readInteraction(item.key)
                        _interactions.update { it + (item.key to interaction) }
                    }
                }
            }
            val existing = if (page == 1) emptySet() else controller(mode).uiState.value.dataList.mapTo(mutableSetOf()) { it.key }
            controller(mode).updateState { it.copy(source = result.source, failed = false) }
            Page(result.items.distinctBy { it.key }.filterNot { it.key in existing }, result.hasMore, result.items.size)
        }

    /** 用户交互意图统一分发入口。 */
    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.SelectMode -> {
                _mode.value = intent.mode
                savedState["feed.mode"] = intent.mode.name
                initialize(intent.mode)
            }
            is FeedIntent.LoadMore -> controller(intent.mode).loadMore()
            is FeedIntent.Refresh -> controller(intent.mode).refresh()
            is FeedIntent.Retry -> {
                val controller = controller(intent.mode)
                if (controller.uiState.value.dataList.isEmpty()) controller.initialize() else controller.loadMore()
            }
            is FeedIntent.ToggleLike -> updateInteraction(intent.key, like = true)
            is FeedIntent.ToggleSave -> updateInteraction(intent.key, like = false)
            is FeedIntent.PlayMusicTrack -> musicController?.playTrack(intent.track, intent.queue)
            FeedIntent.DismissInteractionError -> _interactionError.value = false
        }
    }

    /** 异步更新点赞或收藏持久化状态。 */
    private fun updateInteraction(key: String, like: Boolean) {
        viewModelScope.launch {
            try {
                interactionMutex.withLock {
                    val current = _interactions.value[key] ?: interactionStore.read(key)
                    val next = if (like) current.copy(liked = !current.liked) else current.copy(saved = !current.saved)
                    if (interactionStore.write(key, next)) {
                        _interactions.update { it + (key to next) }
                    } else {
                        _interactionError.value = true
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _interactionError.value = true
            }
        }
    }

    /** 读取单个作品的本地点赞/收藏状态 */
    private suspend fun readInteraction(key: String): FeedInteraction = try {
        interactionStore.read(key)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        _interactionError.value = true
        FeedInteraction()
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
