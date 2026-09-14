package com.lyf.small.feature.creation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.feature.creation.presentation.CreationRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** AI 创作 Feature 的入口路由。 */
@Serializable
data object CreationRoute : NavKey {
    override fun toString(): String = "CreationRoute"
}

val creationNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CreationRoute::class, CreationRoute.serializer())
    }
}

fun EntryProviderScope<NavKey>.creationEntryProvider() {
    entry<CreationRoute> {
        CreationRouteScene()
    }
}
