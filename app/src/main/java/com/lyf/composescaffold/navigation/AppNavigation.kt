package com.lyf.composescaffold.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.R
import com.lyf.composescaffold.core.data.network.SessionEventManager
import com.lyf.composescaffold.core.data.network.SessionState
import com.lyf.composescaffold.core.design.navigation.AppNavHost
import com.lyf.composescaffold.core.design.navigation.TabAppNavHost
import com.lyf.composescaffold.core.design.navigation.TabNavigator
import com.lyf.composescaffold.core.design.navigation.rememberAppNavigator
import com.lyf.composescaffold.core.design.navigation.rememberTabNavigator
import com.lyf.composescaffold.core.design.ui.event.ObserveAsEvents
import com.lyf.composescaffold.feature.browse.navigation.browseEntryProvider
import com.lyf.composescaffold.feature.browse.navigation.browseNavigationSerializers
import com.lyf.composescaffold.feature.cart.navigation.cartEntryProvider
import com.lyf.composescaffold.feature.cart.navigation.cartNavigationSerializers
import com.lyf.composescaffold.feature.home.navigation.homeEntryProvider
import com.lyf.composescaffold.feature.home.navigation.homeNavigationSerializers
import com.lyf.composescaffold.feature.login.navigation.LoginRoute
import com.lyf.composescaffold.feature.login.navigation.loginEntryProvider
import com.lyf.composescaffold.feature.login.navigation.loginNavigationSerializers
import com.lyf.composescaffold.feature.message.navigation.messageEntryProvider
import com.lyf.composescaffold.feature.message.navigation.messageNavigationSerializers
import com.lyf.composescaffold.feature.mine.navigation.mineEntryProvider
import com.lyf.composescaffold.feature.mine.navigation.mineNavigationSerializers
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable
    data object MainTabs : AppRoute {
        override fun toString(): String = "AppRoute.MainTabs"
    }
}

private val mainTabsSerializers: SerializersModule =
    homeNavigationSerializers +
        browseNavigationSerializers +
        messageNavigationSerializers +
        cartNavigationSerializers +
        mineNavigationSerializers

private val rootNavigationSerializers: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(AppRoute.MainTabs::class, AppRoute.MainTabs.serializer())
    }
} + loginNavigationSerializers

private val topLevelTabs = TopLevelTab.entries.map { it.route }

/** 应用根导航承载 Tab Shell 与登录、支付等全局全屏流程。 */
@Composable
fun AppNavigation(sessionEventManager: SessionEventManager? = null) {
    val rootNavigator = rememberAppNavigator(
        startDestination = AppRoute.MainTabs,
        serializersModule = rootNavigationSerializers,
    )

    if (sessionEventManager != null) {
        // StateFlow 在恢复到前台时重放状态；singleTop 避免重复压入登录页。
        ObserveAsEvents(sessionEventManager.state) { state ->
            if (state is SessionState.Expired) {
                rootNavigator.navigate(LoginRoute.Main)
            }
        }
    }

    AppNavHost(
        navigator = rootNavigator,
        modifier = Modifier.fillMaxSize(),
    ) {
        entry<AppRoute.MainTabs> {
            MainTabNavigation(
                onLogin = { rootNavigator.navigate(LoginRoute.Main) },
            )
        }
        loginEntryProvider {
            rootNavigator.navigateBack()
        }
    }
}

/** 五个顶层业务入口各自持有返回栈，切换 Tab 不会清理其他 Tab 的页面状态。 */
@Composable
private fun MainTabNavigation(onLogin: () -> Unit) {
    val navigator = rememberTabNavigator(topLevelTabs, mainTabsSerializers)
    val isTabRoot = navigator.currentRoute in topLevelTabs
    val isHomeRoot = navigator.currentTabIndex == 0 && isTabRoot

    PlatformExitHandler(
        isTabRoot = isTabRoot,
        isHomeRoot = isHomeRoot,
        onNavigateHome = { navigator.switchTab(topLevelTabs.first()) },
    )

    // 底栏常驻底层以稳定页面几何；二级页面出现时禁用其交互和无障碍语义。
    Box(modifier = Modifier.fillMaxSize()) {
        AppBottomBar(
            navigator = navigator,
            enabled = isTabRoot,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        TabAppNavHost(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        ) { _ ->
            homeEntryProvider()
            browseEntryProvider()
            messageEntryProvider()
            cartEntryProvider { _ -> onLogin() }
            mineEntryProvider()
        }
    }
}

@Composable
private fun AppBottomBar(
    navigator: TabNavigator,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.then(
            if (enabled) Modifier else Modifier.clearAndSetSemantics { },
        ),
    ) {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = navigator.currentTab == tab.route,
                onClick = { navigator.switchTab(tab.route) },
                enabled = enabled,
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            TopLevelTab.HOME -> Icons.Default.Home
                            TopLevelTab.BROWSE -> Icons.Default.Search
                            TopLevelTab.MESSAGE -> Icons.Default.Email
                            TopLevelTab.CART -> Icons.Default.ShoppingCart
                            TopLevelTab.MINE -> Icons.Default.Person
                        },
                        // NavigationBarItem 已有可见 label，图标不重复提供语义，避免读屏重复朗读。
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}

private val TopLevelTab.labelRes: Int
    get() = when (this) {
        TopLevelTab.HOME -> R.string.tab_home
        TopLevelTab.BROWSE -> R.string.tab_browse
        TopLevelTab.MESSAGE -> R.string.tab_message
        TopLevelTab.CART -> R.string.tab_cart
        TopLevelTab.MINE -> R.string.tab_mine
    }
