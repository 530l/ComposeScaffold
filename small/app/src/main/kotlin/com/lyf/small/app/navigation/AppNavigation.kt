package com.lyf.small.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lyf.small.core.design.navigation.TabNavHost
import com.lyf.small.core.design.navigation.TabNavigator
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

internal val navigationSerializers =
    exploreNavigationSerializers +
        creationNavigationSerializers +
        assetsNavigationSerializers +
        mineNavigationSerializers

/** 组合四个 Feature 的顶层路由与 Miuix 底部导航；账号流程经 [onNavigateToLogin] 上抛到 App 级路由。 */
@Composable
fun AppNavigation(onNavigateToLogin: () -> Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        TabNavHost(
            navigator = navigator,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
            mineEntryProvider(onNavigateToLogin)
        }

        // 底部栏常驻布局位：页面区域到其上沿为止，转场与页面内容永不触及
        AppTabBar(navigator)
    }
}

/** 常驻底部 Tab 栏：不参与页面显隐与转场，仅在 Tab 切换时重组。 */
@Composable
private fun AppTabBar(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}
