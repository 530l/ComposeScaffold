package com.lyf.composescaffold.feature.home.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.home.presentation.HomeScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 首页模块只暴露路由和 EntryProvider，跨模块跳转由应用组合层连接。 */
@Serializable
sealed interface HomeRoute : NavKey {
    @Serializable
    data object Main : HomeRoute {
        override fun toString(): String = "HomeRoute.Main"
    }
}

val homeNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HomeRoute.Main::class, HomeRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.homeEntryProvider() {
    entry<HomeRoute.Main> {
        HomeScreen()
    }
}
