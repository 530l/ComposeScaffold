package com.lyf.composescaffold.core.design.image

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
import coil3.request.ImageRequest
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.lyf.composescaffold.core.common.log.AppLogger
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

private val LocalAppImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("AppImage 必须位于 ProvideAppImageLoader 内")
}

/**
 * 进程级图片加载器工厂：由组合根（:app 的 Hilt 模块）以 @Singleton 提供，
 * 由组合根传入无认证客户端，可共享连接池，但不能包含业务认证拦截器。
 */
@OptIn(ExperimentalCoilApi::class)
fun createAppImageLoader(
    platformContext: coil3.PlatformContext,
    okHttpClient: OkHttpClient,
): ImageLoader = ImageLoader.Builder(platformContext)
    .components {
        add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
    }
    .build()

/** 挂载应用级 [ImageLoader]（由 Hilt 提供的进程单例），生命周期随进程，不随组合销毁。 */
@Composable
fun ProvideAppImageLoader(
    imageLoader: ImageLoader,
    content: @Composable () -> Unit,
) {
    // 提示阅读者：loader 是进程单例，这里刻意不做 DisposableEffect shutdown。
    CompositionLocalProvider(LocalAppImageLoader provides imageLoader, content = content)
}

/** 统一图片入口，业务层无需了解 Coil、OkHttp 引擎或缓存实例。 */
@Composable
fun AppImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    pixelSize: Int? = null,
) {
    val model = imageUrl
        ?.toHttpUrlOrNull()
        ?.takeIf { url -> url.isHttps }
        ?.toString()
    if (imageUrl != null && model == null) {
        // 地址可能携带签名参数，只记录拒绝事实，不输出原始值。
        AppLogger.debug("AppImage") { "忽略无效或非 HTTPS 图片地址" }
    }
    val context = LocalPlatformContext.current
    val request = remember(context, model, pixelSize) {
        if (pixelSize == null) model else ImageRequest.Builder(context)
            .data(model).size(pixelSize.coerceAtLeast(1)).build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        imageLoader = LocalAppImageLoader.current,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/** 仅预取调用方给出的附近封面；窗口变化或离屏后取消尚未完成的请求。 */
@Composable
fun PrefetchAppImages(imageUrls: List<String>, pixelSize: Int = 640) {
    val loader = LocalAppImageLoader.current
    val context = LocalPlatformContext.current
    DisposableEffect(loader, context, imageUrls, pixelSize) {
        val requests = imageUrls.distinct().mapNotNull { it.toHttpUrlOrNull()?.takeIf { url -> url.isHttps } }
            .map { url -> loader.enqueue(ImageRequest.Builder(context).data(url.toString()).size(pixelSize).build()) }
        onDispose { requests.forEach { it.dispose() } }
    }
}
