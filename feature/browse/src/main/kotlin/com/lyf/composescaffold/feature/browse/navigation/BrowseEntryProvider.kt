package com.lyf.composescaffold.feature.browse.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.browse.presentation.BrowseScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 逛模块只暴露路由和 EntryProvider，跨模块跳转由应用组合层连接。 */
@Serializable
sealed interface BrowseRoute : NavKey {
    @Serializable
    data object Main : BrowseRoute {
        override fun toString(): String = "BrowseRoute.Main"
    }
}

val browseNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BrowseRoute.Main::class, BrowseRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.browseEntryProvider() {
    entry<BrowseRoute.Main> {
        BrowseScreen()
    }
}
