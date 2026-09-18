package com.lyf.small.core.design.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider as navigationEntryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.modules.SerializersModule

/** 每个顶层 Tab 持有独立返回栈，切换 Tab 时保留页面状态和返回历史。 */
@Stable
class TabNavigator internal constructor(
    val tabs: List<NavKey>,
    private val stacks: Map<NavKey, NavBackStack<NavKey>>,
    private val currentIndex: MutableIntState,
    private val mutableReselectEvents: MutableSharedFlow<NavKey>,
) {
    val currentTabIndex: Int
        get() = currentIndex.intValue.coerceIn(tabs.indices)

    val currentTab: NavKey
        get() = tabs[currentTabIndex]

    val currentStack: NavBackStack<NavKey>
        get() = stacks.getValue(currentTab)

    /** 重复点击当前处于选中态的 Tab 时发出事件（供页面执行快速回顶、下拉刷新等）。 */
    val reselectEvents: Flow<NavKey> = mutableReselectEvents.asSharedFlow()

    fun stackOf(tab: NavKey): NavBackStack<NavKey> = stacks.getValue(tab)

    fun switchTab(tab: NavKey) {
        val index = tabs.indexOf(tab)
        require(index >= 0) { "未注册的 Tab 路由: $tab" }
        if (index == currentIndex.intValue) {
            mutableReselectEvents.tryEmit(tab)
            return
        }
        currentIndex.intValue = index
    }

    fun navigate(route: NavKey, singleTop: Boolean = true) {
        if (singleTop && currentStack.lastOrNull() == route) return
        currentStack.add(route)
    }

    fun navigateBack(): Boolean {
        if (currentStack.size <= 1) return false
        currentStack.removeAt(currentStack.lastIndex)
        return true
    }
}

/** 创建可保存进程重建状态的多 Tab Navigation 3 导航器。 */
@Composable
fun rememberTabNavigator(
    tabs: List<NavKey>,
    serializersModule: SerializersModule,
): TabNavigator {
    require(tabs.isNotEmpty()) { "至少需要注册一个顶层 Tab" }
    require(tabs.distinct().size == tabs.size) { "顶层 Tab 路由不能重复" }

    val savedStateConfiguration = remember(serializersModule) {
        SavedStateConfiguration {
            this.serializersModule = serializersModule
        }
    }
    val stacks = tabs.associateWith { tab ->
        key(tab) { rememberNavBackStack(savedStateConfiguration, tab) }
    }
    val currentIndex = rememberSaveable { mutableIntStateOf(0) }
    val reselectEvents = remember { MutableSharedFlow<NavKey>(extraBufferCapacity = 1) }
    return remember(tabs) { TabNavigator(tabs, stacks, currentIndex, reselectEvents) }
}

/** 转场时长：推入与退出共用，保证按钮、手势两种关闭路径节奏一致（参考 chengdongqing/WeChat）。 */
private const val TRANSITION_DURATION_MILLISECOND = 300
private val TRANSITION_ANIMATION_SPEC = tween<IntOffset>(
    durationMillis = TRANSITION_DURATION_MILLISECOND,
)

/** 推入转场：新页面从右侧全宽滑入，底层页面全宽滑出。 */
private fun enterTransition(): ContentTransform = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
) togetherWith slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
)

/** 退出转场：底层页面从左侧全宽复位，当前页面全宽滑出；按钮关闭与侧滑关闭共用同一实现。 */
private fun exitTransition(): ContentTransform = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
) togetherWith slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = TRANSITION_ANIMATION_SPEC,
)

/** Navigation 3 页面容器；Feature 只注册路由入口，不直接操作其他 Feature 的返回栈。 */
public val TabBarHeight: Dp = 80.dp

@Composable
fun TabNavHost(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
    tabBarHeight: Dp = TabBarHeight,
    entryProvider: EntryProviderScope<NavKey>.(navigator: TabNavigator) -> Unit,
) {
    val baseProvider = navigationEntryProvider {
        entryProvider(navigator)
    }
    val stableEntryProvider: (NavKey) -> NavEntry<NavKey> = { route ->
        val entry = baseProvider(route)
        val isTabRoot = route in navigator.tabs
        NavEntry(
            key = route,
            contentKey = route.toString(),
            metadata = entry.metadata,
        ) {
            Box(
                modifier = if (isTabRoot) {
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = tabBarHeight)
                } else {
                    Modifier.fillMaxSize()
                },
            ) {
                entry.Content()
            }
        }
    }
    val decoratedEntries = navigator.tabs.map { tab ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = navigator.stackOf(tab),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = stableEntryProvider,
            )
        }
    }

    val activeEntries = decoratedEntries[navigator.currentTabIndex]
    val tabRootContentKeys: Set<Any> = remember(navigator.tabs) {
        navigator.tabs.mapTo(mutableSetOf()) { it.toString() }
    }

    NavDisplay(
        entries = activeEntries,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        transitionSpec = {
            val fromRoot = initialState.entries.lastOrNull()?.contentKey
                ?.let { it in tabRootContentKeys } == true
            val toRoot = targetState.entries.lastOrNull()?.contentKey
                ?.let { it in tabRootContentKeys } == true
            if (fromRoot && toRoot) {
                // 顶层 Tab 之间是直接切换，不属于页面导航，不播放转场动画
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                enterTransition()
            }
        },
        popTransitionSpec = { exitTransition() },
        predictivePopTransitionSpec = { exitTransition() },
    )
}
