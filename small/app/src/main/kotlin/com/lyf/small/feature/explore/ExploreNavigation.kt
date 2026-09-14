package com.lyf.small.feature.explore

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.feature.explore.home.ExploreRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** 探索 Feature 的入口路由。 */
@Serializable
data object ExploreRoute : NavKey {
    override fun toString(): String = "ExploreRoute"
}

val exploreNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ExploreRoute::class, ExploreRoute.serializer())
    }
}

fun EntryProviderScope<NavKey>.exploreEntryProvider() {
    entry<ExploreRoute> {
        ExploreRouteScene()
    }
}
