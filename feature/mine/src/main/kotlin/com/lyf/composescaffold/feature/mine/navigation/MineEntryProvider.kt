package com.lyf.composescaffold.feature.mine.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.mine.presentation.MineScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 我的模块只暴露路由和 EntryProvider，不向其他 Feature 暴露页面拼装细节。 */
@Serializable
sealed interface MineRoute : NavKey {
    // Nav3 用 key.toString() 作 contentKey（saveable 状态与 entry 级 ViewModelStore 的存取键），
    // data object 默认 toString 只有 "Main"，跨 feature 会互相覆盖/误删，必须限定名。
    @Serializable
    data object Main : MineRoute {
        override fun toString(): String = "MineRoute.Main"
    }
}

val mineNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(MineRoute.Main::class, MineRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.mineEntryProvider() {
    entry<MineRoute.Main> {
        MineScreen()
    }
}
