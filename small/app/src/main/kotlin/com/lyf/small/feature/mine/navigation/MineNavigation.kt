package com.lyf.small.feature.mine.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.feature.mine.presentation.MineRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 我的 Feature 的入口路由。 */
@Serializable
data object MineRoute : NavKey {
    override fun toString(): String = "MineRoute"
}

val mineNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MineRoute::class, MineRoute.serializer())
    }
}

fun EntryProviderScope<NavKey>.mineEntryProvider() {
    entry<MineRoute> {
        MineRouteScene()
    }
}
