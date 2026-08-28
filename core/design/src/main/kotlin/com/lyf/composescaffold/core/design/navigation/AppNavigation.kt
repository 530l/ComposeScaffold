package com.lyf.composescaffold.core.design.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider as navigationEntryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule

/** 统一管理 Navigation 3 返回栈，Feature 不直接持有或修改 back stack。 */
class AppNavigator internal constructor(
    private val backStack: NavBackStack<NavKey>,
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
    val routeSerializers = serializersModule
    val savedStateConfiguration = remember(routeSerializers) {
        SavedStateConfiguration {
            this.serializersModule = routeSerializers
        }
    }
    val backStack = rememberNavBackStack(savedStateConfiguration, startDestination)
    val navigator = remember(backStack) { AppNavigator(backStack) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        transitionSpec = { forwardContentTransform() },
        popTransitionSpec = { popContentTransform() },
        predictivePopTransitionSpec = { edge -> predictivePopContentTransform(edge) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = navigationEntryProvider {
            entryProvider(navigator)
        },
    )
}
