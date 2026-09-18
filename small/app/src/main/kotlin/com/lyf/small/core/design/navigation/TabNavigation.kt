package com.lyf.small.core.design.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.navigationevent.NavigationEvent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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

/** Navigation 3 页面容器；Feature 只注册路由入口，不直接操作其他 Feature 的返回栈。 */
public val TabBarHeight: Dp = 80.dp
public const val MIUIX_TRANSITION_DURATION_MS: Int = 500

/**
 * 提取自 /Users/a530/Desktop/kmpcmp/miuix 的 Miuix 官方二阶欠阻尼弹簧曲线。
 * 前 20% 时间迅猛位移 48%，后半段呈指数级柔和减速，兼具极速响应与物理丝滑感。
 */
@Immutable
class MiuixNavEasing(
    response: Float = 0.8f,
    damping: Float = 0.95f,
) : androidx.compose.animation.core.Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * kotlin.math.PI / response
        val k = omega * omega
        val c = damping * 4.0 * kotlin.math.PI / response

        w = (kotlin.math.sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = kotlin.math.exp(r * t)
        return (decay * (-kotlin.math.cos(w * t) + c2 * kotlin.math.sin(w * t)) + 1.0).toFloat()
    }
}

public val MiuixTransitionEasing: androidx.compose.animation.core.Easing =
    MiuixNavEasing(response = 0.8f, damping = 0.95f)

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
                // 打开新页面统一使用 Miuix 物理推入转场
                miuixPushTransition()
            }
        },
        popTransitionSpec = {
            // 点击返回按钮、物理返回键等退出统一使用 Miuix 物理退出转场
            miuixPopTransition()
        },
        predictivePopTransitionSpec = { edge ->
            // 边缘手势侧滑返回：手势拖拽严格 1:1 跟手平移，杜绝非线性跳变与缩放变形
            val direction = if (edge == NavigationEvent.EDGE_LEFT) {
                AnimatedContentTransitionScope.SlideDirection.End
            } else {
                AnimatedContentTransitionScope.SlideDirection.Start
            }
            slideIntoContainer(
                towards = direction,
                initialOffset = { it / 4 },
                animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = LinearEasing),
            ) togetherWith slideOutOfContainer(
                towards = direction,
                animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = LinearEasing),
            )
        },
    )
}

/**
 * Miuix 统一页面推入转场（Push / 打开新页面）：
 * - 目标页面从屏幕右侧完全滑入（100% -> 0）；
 * - 原页面向屏幕左侧进行 25% 视差位移（0 -> -25%）；
 * - 运动曲线使用 Miuix 官方二阶欠阻尼物理弹簧（前 20% 时间迅猛位移 48%，后半段丝滑吸合）。
 */
fun <T : Any> AnimatedContentTransitionScope<T>.miuixPushTransition(): ContentTransform {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = MiuixTransitionEasing),
    ) togetherWith slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = MiuixTransitionEasing),
        targetOffset = { it / 4 },
    )
}

/**
 * Miuix 统一页面退出转场（Pop / 关闭当前页面）：
 * 无论用户通过以下何种方式触发关闭：
 * 1. 点击标题栏或界面内的返回 / 关闭按钮；
 * 2. 点击手机系统物理按键或虚拟返回键；
 * 3. 屏幕边缘手势侧滑返回（Predictive Back）。
 * 动效全部统一为：
 * - 底层页面从左侧 25% 视差位置平滑复位（-25% -> 0）；
 * - 当前页面完全向右滑出屏幕（0 -> 100%）；
 * - 动效曲线完全共享 Miuix 官方二阶欠阻尼物理弹簧，彻底消除手势与点击动作之间的割裂感。
 */
fun <T : Any> AnimatedContentTransitionScope<T>.miuixPopTransition(): ContentTransform {
    return slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = MiuixTransitionEasing),
        initialOffset = { it / 4 },
    ) togetherWith slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(MIUIX_TRANSITION_DURATION_MS, easing = MiuixTransitionEasing),
    )
}
