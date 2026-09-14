package com.lyf.small.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.lyf.small.R
import com.lyf.small.feature.assets.navigation.AssetsRoute
import com.lyf.small.feature.creation.navigation.CreationRoute
import com.lyf.small.feature.explore.ExploreRoute
import com.lyf.small.feature.mine.navigation.MineRoute
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.Search
import top.yukonga.miuix.kmp.icon.icons.useful.Edit
import top.yukonga.miuix.kmp.icon.icons.useful.Order
import top.yukonga.miuix.kmp.icon.icons.useful.Personal

/** 顶层 Tab 仅保存应用级展示信息，具体页面入口仍由各 Feature 注册。 */
enum class TopLevelTab(
    val route: NavKey,
    val labelRes: Int,
    val icon: ImageVector,
) {
    EXPLORE(
        route = ExploreRoute,
        labelRes = R.string.app_tab_explore,
        icon = MiuixIcons.Basic.Search,
    ),
    CREATION(
        route = CreationRoute,
        labelRes = R.string.app_tab_creation,
        icon = MiuixIcons.Useful.Edit,
    ),
    ASSETS(
        route = AssetsRoute,
        labelRes = R.string.app_tab_assets,
        icon = MiuixIcons.Useful.Order,
    ),
    MINE(
        route = MineRoute,
        labelRes = R.string.app_tab_mine,
        icon = MiuixIcons.Useful.Personal,
    ),
}
