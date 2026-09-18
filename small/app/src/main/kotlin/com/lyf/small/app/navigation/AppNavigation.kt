package com.lyf.small.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lyf.small.core.design.navigation.TabNavHost
import com.lyf.small.core.design.navigation.rememberTabNavigator
import com.lyf.small.feature.assets.navigation.assetsEntryProvider
import com.lyf.small.feature.assets.navigation.assetsNavigationSerializers
import com.lyf.small.feature.creation.navigation.creationEntryProvider
import com.lyf.small.feature.creation.navigation.creationNavigationSerializers
import com.lyf.small.feature.explore.ArticleDetailRoute
import com.lyf.small.feature.explore.exploreEntryProvider
import com.lyf.small.feature.explore.exploreNavigationSerializers
import com.lyf.small.feature.mine.navigation.mineEntryProvider
import com.lyf.small.feature.mine.navigation.mineNavigationSerializers
import kotlinx.serialization.modules.plus
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
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
    val isTabRoot = navigator.currentStack.size == 1
    val isHomeRoot = navigator.currentTabIndex == 0 && isTabRoot

    PlatformExitHandler(
        isTabRoot = isTabRoot,
        isHomeRoot = isHomeRoot,
        onNavigateHome = { navigator.switchTab(topLevelTabs.first()) },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        TabNavHost(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        ) {
            exploreEntryProvider(
                onNavigateToArticle = { article ->
                    navigator.navigate(
                        ArticleDetailRoute(
                            articleId = article.id,
                            title = article.title,
                            author = article.author,
                        ),
                    )
                },
                onBack = { navigator.navigateBack() },
            )
            creationEntryProvider()
            assetsEntryProvider()
            mineEntryProvider()
        }

        AnimatedVisibility(
            visible = isTabRoot,
            enter = fadeIn(animationSpec = tween(180, delayMillis = 260)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
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
        }
    }
}
