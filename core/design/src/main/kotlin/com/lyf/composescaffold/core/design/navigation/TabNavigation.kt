package com.lyf.composescaffold.core.design.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
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

/**
 * 多 tab 导航容器：每个 tab 持有独立返回栈，切 tab 只切换当前栈引用，
 * 各 tab 的页面状态、entryDecorators 与返回历史互不影响，切回 tab 时 saveable 状态与
 * Entry 级 ViewModel 自动恢复。
 */
@Stable
class TabNavigator internal constructor(
    val tabs: List<NavKey>,
    private val stacks: Map<NavKey, NavBackStack<NavKey>>,
    private val currentIndex: MutableIntState,
    private val mutableReselectEvents: MutableSharedFlow<NavKey>,
) {
    /** 当前选中的 tab 根路由；tab 集合变动导致索引越界时回落到首个 tab。 */
    val currentTab: NavKey
        get() = tabs.getOrElse(currentIndex.intValue) { tabs.first() }

    /** 当前 tab 索引；读取即订阅，切 tab 会触发宿主重组。 */
    val currentTabIndex: Int
        get() = currentIndex.intValue.coerceIn(tabs.indices)

    val currentStack: NavBackStack<NavKey>
        get() = stacks.getValue(currentTab)

    val currentRoute: NavKey
        get() = currentStack.last()

    /** 重复点击当前 tab 时发出事件，页面自行决定滚动到顶部、刷新或保持原状。 */
    val reselectEvents: Flow<NavKey> = mutableReselectEvents.asSharedFlow()

    /** 各 tab 的返回栈；装饰 entry 时每个栈需要独立的 rememberDecoratedNavEntries 调用点。 */
    fun stackOf(tab: NavKey): NavBackStack<NavKey> = stacks.getValue(tab)

    fun switchTab(tab: NavKey) {
        val index = tabs.indexOf(tab)
        require(index >= 0) { "未注册的 tab 路由: $tab" }
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
        val stack = currentStack
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

/**
 * tabs 必须是稳定的顶层列表（tab 集合在运行期不变），tab 根路由即各栈栈底。
 * 每个栈的进程重建恢复由 rememberNavBackStack 自身的持久化能力承担。
 */
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

/**
 * Material3 NavigationBar 的固定内容高度。shell 的底栏常驻页面底层（overlay），
 * tab 根页面按「栏高 + 手势区 inset」让位；换用非标准高度底栏时需把实际高度传入
 * [TabAppNavHost]。
 */
public val TabBarContentHeight: Dp = 80.dp

/**
 * Tab 版导航容器；entryProvider 回调里通过 navigator 完成跨 Feature 跳转。
 *
 * 每个 tab 的返回栈走独立的 [rememberDecoratedNavEntries] 调用点（各自的弹出追踪集合），
 * NavDisplay 只接收当前 tab entries；从其他 Tab 返回首页由平台返回处理器直接切换，
 * 不把 Tab 切换伪装成页面出栈，saveable 状态与 entry 级 ViewModelStore 得以保留。
 * Tab 页面首次进入时才创建 ViewModel；切换 Tab 或被登录页等全屏页面覆盖不会清理状态，
 * 只有 MainTabs 根 Entry 真正出栈时才统一释放。
 *
 * 页面与底栏不共享几何：tab 根页面在 entry 层让出 [tabBarContentHeight] + 手势区，
 * 推入页整页不透明、直接盖住底栏。底栏因此不参与页面布局，页面进入/返回
 * 不会因底栏收起而发生二次跳动。
 */
@Composable
fun TabAppNavHost(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
    tabBarContentHeight: Dp = TabBarContentHeight,
    entryProvider: EntryProviderScope<NavKey>.(navigator: TabNavigator) -> Unit,
) {
    val mainTabsOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "TabAppNavHost 必须位于 ViewModelStoreOwner 作用域内"
    }
    // 绑定 MainTabs 根 Entry：全屏页面覆盖时保留各 Tab store，MainTabs 出栈时统一清理。
    val tabViewModelStoreProvider = remember(mainTabsOwner) {
        ViewModelStoreProvider(
            parentOwner = mainTabsOwner,
            parentKey = "TabAppNavHost",
        )
    }
    val tabRootOwners: Map<NavKey, ViewModelStoreOwner> = navigator.tabs.associateWith { tab ->
        key(tab) {
            // 每个 Tab 根路由持有独立 owner；首次进入才创建 ViewModel，切换时不重建。
            rememberViewModelStoreOwner(
                key = tab.toString(),
                provider = tabViewModelStoreProvider,
            )
        }
    }
    val baseProvider = navigationEntryProvider {
        entryProvider(navigator)
    }
    val pageBackground = MaterialTheme.colorScheme.background
    val navigationBarInsets = WindowInsets.navigationBars
    val pageProvider: (NavKey) -> NavEntry<NavKey> = { key ->
        val entry = baseProvider(key)
        val pageModifier = if (key in navigator.tabs) {
            // tab 根页面：让位区保持透明，底栏（叠在页面底层）透出且可点
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(navigationBarInsets)
                .padding(bottom = tabBarContentHeight)
        } else {
            // 推入页：整页不透明，覆盖底栏
            Modifier.fillMaxSize()
        }
        NavEntry(navEntry = entry) {
            Box(
                modifier = pageModifier.background(pageBackground),
            ) {
                if (key in navigator.tabs) {
                    // Tab 根页面固定使用自己的 owner，重组或重新进入 composition 只恢复原状态。
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides tabRootOwners.getValue(key),
                    ) {
                        entry.Content()
                    }
                } else {
                    entry.Content()
                }
            }
        }
    }
    val decoratedEntries = navigator.tabs.map { tab ->
        key(tab) {
            // 每个返回栈持有独立的状态与 ViewModel 装饰器，避免跨 Tab 清理彼此的 Entry。
            val decorators: List<NavEntryDecorator<NavKey>> = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                remember(tabViewModelStoreProvider, tab) {
                    ViewModelStoreNavEntryDecorator(tabViewModelStoreProvider)
                },
            )
            rememberDecoratedNavEntries(
                backStack = navigator.stackOf(tab),
                entryDecorators = decorators,
                entryProvider = pageProvider,
            )
        }
    }
    val activeEntries = decoratedEntries[navigator.currentTabIndex]
    val tabRootContentKeys = remember(navigator.tabs) { navigator.tabs.map { it.toString() }.toSet() }
    NavDisplay(
        entries = activeEntries,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        transitionSpec = {
            val fromRoot = initialState.entries.lastOrNull()?.contentKey
                ?.let { it.toString() in tabRootContentKeys } == true
            val toRoot = targetState.entries.lastOrNull()?.contentKey
                ?.let { it.toString() in tabRootContentKeys } == true
            if (fromRoot && toRoot) {
                // 顶层 Tab 之间是直接切换，不属于页面导航，不播放转场动画。
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                forwardContentTransform()
            }
        },
        popTransitionSpec = { popContentTransform() },
        predictivePopTransitionSpec = { edge -> predictivePopContentTransform(edge) },
    )
}
