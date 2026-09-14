package com.lyf.small.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lyf.small.core.design.navigation.TabNavHost
import com.lyf.small.core.design.navigation.rememberTabNavigator
import com.lyf.small.feature.assets.navigation.assetsEntryProvider
import com.lyf.small.feature.assets.navigation.assetsNavigationSerializers
import com.lyf.small.feature.creation.navigation.creationEntryProvider
import com.lyf.small.feature.creation.navigation.creationNavigationSerializers
import com.lyf.small.feature.explore.exploreEntryProvider
import com.lyf.small.feature.explore.exploreNavigationSerializers
import com.lyf.small.feature.mine.navigation.mineEntryProvider
import com.lyf.small.feature.mine.navigation.mineNavigationSerializers
import kotlinx.serialization.modules.plus
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val topLevelTabs = TopLevelTab.entries.map { it.route }

private val navigationSerializers =
    exploreNavigationSerializers +
        creationNavigationSerializers +
        assetsNavigationSerializers +
        mineNavigationSerializers

/** 组合四个 Feature 的顶层路由与 Miuix 底部导航。 */
@Composable
fun AppNavigation() {
    val navigator = rememberTabNavigator(
        tabs = topLevelTabs,
        serializersModule = navigationSerializers,
    )
    BackHandler(
        enabled = navigator.currentStack.size == 1 && navigator.currentTabIndex != 0,
    ) {
        navigator.switchTab(topLevelTabs.first())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                items = TopLevelTab.entries.map { tab ->
                    NavigationItem(
                        label = stringResource(tab.labelRes),
                        icon = tab.icon,
                    )
                },
                selected = navigator.currentTabIndex,
                onClick = { index -> navigator.switchTab(TopLevelTab.entries[index].route) },
            )
        },
        containerColor = MiuixTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { contentPadding ->
        TabNavHost(
            navigator = navigator,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
        ) {
            exploreEntryProvider()
            creationEntryProvider()
            assetsEntryProvider()
            mineEntryProvider()
        }
    }
}
