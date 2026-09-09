package com.lyf.composescaffold.feature.browse.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.browse.presentation.BrowseScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Browse 对外提供的路由类型。 */
@Serializable
sealed interface BrowseRoute : NavKey {
    @Serializable
    data object Main : BrowseRoute {
        // 日志与调试中的稳定名称，模块前缀避免与其他模块路由混淆。
        override fun toString(): String = "BrowseRoute.Main"
    }
}

/** 注册 Browse 路由的保存与恢复方式。 */
val browseNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BrowseRoute.Main::class, BrowseRoute.Main.serializer())
    }
}

/** 把 Browse 根页面注册到宿主导航。 */
fun EntryProviderScope<NavKey>.browseEntryProvider(playbackVisible: () -> Boolean = { true }) {
    entry<BrowseRoute.Main> {
        // 组合时读取宿主可见性：被宿主导航遮挡时页面内播放让位。
        BrowseScreen(playbackVisible = playbackVisible())
    }
}
