package com.lyf.composescaffold.feature.message.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.message.presentation.MessageScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 消息模块只暴露路由和 EntryProvider，跨模块跳转由应用组合层连接。 */
@Serializable
sealed interface MessageRoute : NavKey {
    @Serializable
    data object Main : MessageRoute {
        override fun toString(): String = "MessageRoute.Main"
    }
}

val messageNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MessageRoute.Main::class, MessageRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.messageEntryProvider() {
    entry<MessageRoute.Main> {
        MessageScreen()
    }
}
