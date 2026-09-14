package com.lyf.small.core.design.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
import kotlinx.serialization.modules.SerializersModule

/** 每个顶层 Tab 持有独立返回栈，切换 Tab 时保留页面状态和返回历史。 */
@Stable
class TabNavigator internal constructor(
    val tabs: List<NavKey>,
    private val stacks: Map<NavKey, NavBackStack<NavKey>>,
    private val currentIndex: MutableIntState,
) {
    val currentTabIndex: Int
        get() = currentIndex.intValue.coerceIn(tabs.indices)

    val currentTab: NavKey
        get() = tabs[currentTabIndex]

    val currentStack: NavBackStack<NavKey>
        get() = stacks.getValue(currentTab)

    fun stackOf(tab: NavKey): NavBackStack<NavKey> = stacks.getValue(tab)

    fun switchTab(tab: NavKey) {
        val index = tabs.indexOf(tab)
        require(index >= 0) { "未注册的 Tab 路由: $tab" }
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
    return remember(tabs) { TabNavigator(tabs, stacks, currentIndex) }
}

/** Navigation 3 页面容器；Feature 只注册路由入口，不直接操作其他 Feature 的返回栈。 */
@Composable
fun TabNavHost(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
    entryProvider: EntryProviderScope<NavKey>.(navigator: TabNavigator) -> Unit,
) {
    val baseProvider = navigationEntryProvider {
        entryProvider(navigator)
    }
    val stableEntryProvider: (NavKey) -> NavEntry<NavKey> = { route ->
        val entry = baseProvider(route)
        NavEntry(
            key = route,
            contentKey = route.toString(),
            metadata = entry.metadata,
        ) {
            entry.Content()
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

    NavDisplay(
        entries = decoratedEntries[navigator.currentTabIndex],
        modifier = modifier,
        onBack = { navigator.navigateBack() },
    )
}
