package com.lyf.small.feature.explore

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.lyf.small.data.content.model.Article
import com.lyf.small.feature.explore.detail.ArticleDetailRouteScene
import com.lyf.small.feature.explore.home.ExploreRouteScene
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** 探索 Feature 的入口路由。 */
@Serializable
data object ExploreRoute : NavKey {
    override fun toString(): String = "ExploreRoute"
}

/** 文章详情页路由。 */
@Serializable
data class ArticleDetailRoute(
    val articleId: Long,
    val title: String = "",
    val author: String = "",
) : NavKey {
    override fun toString(): String = "ArticleDetailRoute/$articleId"
}

val exploreNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(ExploreRoute::class, ExploreRoute.serializer())
        subclass(ArticleDetailRoute::class, ArticleDetailRoute.serializer())
    }
}

fun EntryProviderScope<NavKey>.exploreEntryProvider(
    onNavigateToArticle: (Article) -> Unit = {},
    onBack: () -> Unit = {},
) {
    entry<ExploreRoute> {
        ExploreRouteScene(
            onArticleClick = onNavigateToArticle,
        )
    }
    entry<ArticleDetailRoute> { route ->
        ArticleDetailRouteScene(
            route = route,
            onBack = onBack,
        )
    }
}
