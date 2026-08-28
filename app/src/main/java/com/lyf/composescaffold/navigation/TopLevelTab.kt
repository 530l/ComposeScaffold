package com.lyf.composescaffold.navigation

import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.browse.navigation.BrowseRoute
import com.lyf.composescaffold.feature.cart.navigation.CartRoute
import com.lyf.composescaffold.feature.home.navigation.HomeRoute
import com.lyf.composescaffold.feature.message.navigation.MessageRoute
import com.lyf.composescaffold.feature.mine.navigation.MineRoute

/**
 * 底部 tab 的顺序与根路由注册点；文案、图标、序列化器和 EntryProvider 由 AppNavigation 聚合，
 * 各 Feature 依旧互不感知。
 */
enum class TopLevelTab(val route: NavKey) {
    HOME(HomeRoute.Main),
    BROWSE(BrowseRoute.Main),
    MESSAGE(MessageRoute.Main),
    CART(CartRoute.Main),
    MINE(MineRoute.Main),
}
