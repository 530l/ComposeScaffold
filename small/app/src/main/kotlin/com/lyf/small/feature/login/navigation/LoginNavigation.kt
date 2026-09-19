package com.lyf.small.feature.login.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.feature.login.forgot.ForgotRouteScene
import com.lyf.small.feature.login.signin.SignInRouteScene
import com.lyf.small.feature.login.signup.SignUpRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 登录页路由：App 级全屏页，盖住底部 Tab 栏。 */
@Serializable
data object LoginRoute : NavKey {
    override fun toString(): String = "LoginRoute"
}

/** 注册页路由。 */
@Serializable
data object SignUpRoute : NavKey {
    override fun toString(): String = "SignUpRoute"
}

/** 忘记密码页路由：验证与重置两步在同一页内步进。 */
@Serializable
data object ForgotRoute : NavKey {
    override fun toString(): String = "ForgotRoute"
}

val loginNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(LoginRoute::class, LoginRoute.serializer())
        subclass(SignUpRoute::class, SignUpRoute.serializer())
        subclass(ForgotRoute::class, ForgotRoute.serializer())
    }
}

/**
 * 登录系页面入口。三个 RouteScene 的签名是跨包契约，改动须同步各子包实现：
 * - [SignInRouteScene](onLoginSucceeded, onNavigateToSignUp, onNavigateToForgot)
 * - [SignUpRouteScene](onBack, onRegisterSucceeded)
 * - [ForgotRouteScene](onBack, onResetSucceeded)
 */
fun EntryProviderScope<NavKey>.loginEntryProvider(
    onLoginSucceeded: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onBack: () -> Unit,
) {
    entry<LoginRoute> {
        SignInRouteScene(
            onLoginSucceeded = onLoginSucceeded,
            onNavigateToSignUp = onNavigateToSignUp,
            onNavigateToForgot = onNavigateToForgot,
        )
    }
    entry<SignUpRoute> {
        SignUpRouteScene(
            onBack = onBack,
            onRegisterSucceeded = onBack,
        )
    }
    entry<ForgotRoute> {
        ForgotRouteScene(
            onBack = onBack,
            onResetSucceeded = onBack,
        )
    }
}
