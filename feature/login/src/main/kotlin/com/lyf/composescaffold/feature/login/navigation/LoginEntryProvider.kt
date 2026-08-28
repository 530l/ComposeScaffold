package com.lyf.composescaffold.feature.login.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.login.presentation.LoginScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 登录模块只暴露路由和 EntryProvider，不向其他 Feature 暴露页面拼装细节。 */
@Serializable
sealed interface LoginRoute : NavKey {
    // 导航宿主统一用 key.toString() 作 contentKey（saveable 状态与 ViewModelStore 的存取键），
    // data object 默认 toString 只有 "Main"，跨 feature 会互相覆盖/误删，必须限定名。
    @Serializable
    data object Main : LoginRoute {
        override fun toString(): String = "LoginRoute.Main"
    }
}

val loginNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LoginRoute.Main::class, LoginRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.loginEntryProvider(onBack: () -> Unit) {
    entry<LoginRoute.Main> {
        LoginScreen(onBack = onBack)
    }
}
