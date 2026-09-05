package com.lyf.composescaffold.core.design.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider as navigationEntryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule

/** 统一管理 Navigation 3 返回栈，Feature 不直接持有或修改 back stack。 */
class AppNavigator internal constructor(
    internal val backStack: NavBackStack<NavKey>,
) {
    val currentRoute: NavKey
        get() = backStack.last()

    fun navigate(route: NavKey, singleTop: Boolean = true) {
        if (singleTop && backStack.lastOrNull() == route) return
        backStack.add(route)
    }

    fun navigateBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun replaceAll(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }
}

/** 构造并记忆根级 [AppNavigator]。 */
@Composable
fun rememberAppNavigator(
    startDestination: NavKey,
    serializersModule: SerializersModule,
): AppNavigator {
    val savedStateConfiguration = remember(serializersModule) {
        SavedStateConfiguration {
            this.serializersModule = serializersModule
        }
    }
    val backStack = rememberNavBackStack(savedStateConfiguration, startDestination)
    return remember(backStack) { AppNavigator(backStack) }
}

/**
 * 应用级 Navigation 3 容器：负责跨平台状态恢复、返回栈和 Entry 级 ViewModel 生命周期。
 */
@Composable
fun AppNavHost(
    startDestination: NavKey,
    serializersModule: SerializersModule,
    modifier: Modifier = Modifier,
    entryProvider: EntryProviderScope<NavKey>.(navigator: AppNavigator) -> Unit,
) {
    val navigator = rememberAppNavigator(startDestination, serializersModule)
    AppNavHost(
        navigator = navigator,
        modifier = modifier,
        entryProvider = entryProvider,
    )
}

/**
 * 接收已存在的 [navigator] 实例的 [AppNavHost] 重载，便于在宿主层监听全局会话失效事件并控制导航。
 */
@Composable
fun AppNavHost(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    entryProvider: EntryProviderScope<NavKey>.(navigator: AppNavigator) -> Unit,
) {
    val baseProvider = navigationEntryProvider {
        entryProvider(navigator)
    }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        transitionSpec = { forwardContentTransform() },
        popTransitionSpec = { popContentTransform() },
        predictivePopTransitionSpec = { edge -> predictivePopContentTransform(edge) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { key ->
            val entry = baseProvider(key)
            // Navigation 3 默认 contentKey 的实现可能随版本变化，应用统一使用稳定路由名。
            NavEntry(
                key = key,
                contentKey = key.toString(),
                metadata = entry.metadata,
            ) {
                entry.Content()
            }
        },
    )
}
