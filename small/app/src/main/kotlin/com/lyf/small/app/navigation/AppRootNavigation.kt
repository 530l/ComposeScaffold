package com.lyf.small.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider as navigationEntryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.lyf.small.core.design.navigation.horizontalPopTransition
import com.lyf.small.core.design.navigation.horizontalPushTransition
import com.lyf.small.feature.login.navigation.ForgotRoute
import com.lyf.small.feature.login.navigation.LoginRoute
import com.lyf.small.feature.login.navigation.SignUpRoute
import com.lyf.small.feature.login.navigation.loginEntryProvider
import com.lyf.small.feature.login.navigation.loginNavigationSerializers
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 主界面壳路由：App 级返回栈的根，承载四个 Tab 的完整结构。 */
@Serializable
private data object MainRoute : NavKey {
    override fun toString(): String = "MainRoute"
}

private val rootNavigationSerializers = navigationSerializers +
    loginNavigationSerializers +
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(MainRoute::class, MainRoute.serializer())
        }
    }

/** App 级导航：主界面与全屏账号流程（登录/注册/找回密码）共用一个返回栈与侧滑转场。 */
@Composable
fun AppRootNavigation() {
    val savedStateConfiguration = remember(rootNavigationSerializers) {
        SavedStateConfiguration {
            serializersModule = rootNavigationSerializers
        }
    }
    val backStack = rememberNavBackStack(savedStateConfiguration, MainRoute)

    fun navigate(route: NavKey) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val baseProvider = navigationEntryProvider {
        entry<MainRoute> {
            AppNavigation(onNavigateToLogin = { navigate(LoginRoute) })
        }
        loginEntryProvider(
            onLoginSucceeded = ::navigateBack,
            onNavigateToSignUp = { navigate(SignUpRoute) },
            onNavigateToForgot = { navigate(ForgotRoute) },
            onBack = ::navigateBack,
        )
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
    val decoratedEntries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = stableEntryProvider,
    )

    NavDisplay(
        entries = decoratedEntries,
        onBack = ::navigateBack,
        transitionSpec = { horizontalPushTransition() },
        popTransitionSpec = { horizontalPopTransition() },
        predictivePopTransitionSpec = { horizontalPopTransition() },
    )
}
