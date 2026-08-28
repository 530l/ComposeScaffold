package com.lyf.composescaffold.feature.cart.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.cart.presentation.CartScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 购物车模块拥有自己的路由集合，其他 Feature 不直接引用其页面实现。 */
@Serializable
sealed interface CartRoute : NavKey {
    // Nav3 用 key.toString() 作 contentKey（saveable 状态与 entry 级 ViewModelStore 的存取键），
    // data object 默认 toString 只有 "Main"，跨 feature 会互相覆盖/误删，必须限定名。
    @Serializable
    data object Main : CartRoute {
        override fun toString(): String = "CartRoute.Main"
    }
}

val cartNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(CartRoute.Main::class, CartRoute.Main.serializer())
    }
}

fun EntryProviderScope<NavKey>.cartEntryProvider(
    onCheckout: (selectedItemIds: List<Long>) -> Unit,
) {
    entry<CartRoute.Main> {
        CartScreen(onCheckout = onCheckout)
    }
}
