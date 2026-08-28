package com.lyf.composescaffold.core.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

private val LocalAppImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("AppImage 必须位于 ProvideAppImageLoader 内")
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProvideAppImageLoader(
    okHttpClient: OkHttpClient = OkHttpClient(),
    content: @Composable () -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext, okHttpClient) {
        ImageLoader.Builder(platformContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }

    DisposableEffect(imageLoader) {
        onDispose(imageLoader::shutdown)
    }

    CompositionLocalProvider(LocalAppImageLoader provides imageLoader, content = content)
}

/** 统一图片入口，业务层无需了解 Coil、OkHttp 引擎或缓存实例。 */
@Composable
fun AppImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = imageUrl?.takeIf { it.startsWith("https://") },
        contentDescription = contentDescription,
        imageLoader = LocalAppImageLoader.current,
        modifier = modifier,
        contentScale = contentScale,
    )
}
