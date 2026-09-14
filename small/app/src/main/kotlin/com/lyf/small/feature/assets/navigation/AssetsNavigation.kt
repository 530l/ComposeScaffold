package com.lyf.small.feature.assets.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.feature.assets.presentation.AssetsRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 资产 Feature 的入口路由。 */
@Serializable
data object AssetsRoute : NavKey {
    override fun toString(): String = "AssetsRoute"
}

val assetsNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(AssetsRoute::class, AssetsRoute.serializer())
    }
}

fun EntryProviderScope<NavKey>.assetsEntryProvider() {
    entry<AssetsRoute> {
        AssetsRouteScene()
    }
}
