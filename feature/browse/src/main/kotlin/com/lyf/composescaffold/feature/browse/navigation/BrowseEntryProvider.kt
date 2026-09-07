package com.lyf.composescaffold.feature.browse.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.composescaffold.feature.browse.presentation.BrowseScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** 浏览业务模块对外公开的导航契约与节点装配入口（BrowseEntryProvider）。 */
@Serializable
sealed interface BrowseRoute : NavKey {
    /** 浏览模块根路由节点。 */
    @Serializable
    data object Main : BrowseRoute {
        override fun toString(): String = "BrowseRoute.Main"
    }
}

/** 浏览模块导航序列化注册表，用于 Nav3 进程重建与跨页面跳转状态保存。 */
val browseNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(BrowseRoute.Main::class, BrowseRoute.Main.serializer())
    }
}

/** 向宿主应用导航栈提供 Browse 模块的视图渲染节点。 */
fun EntryProviderScope<NavKey>.browseEntryProvider(playbackVisible: () -> Boolean = { true }) {
    entry<BrowseRoute.Main> {
        BrowseScreen(playbackVisible = playbackVisible())
    }
}
