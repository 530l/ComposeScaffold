package com.lyf.small.app

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import com.lyf.small.app.navigation.AppRootNavigation
import com.lyf.small.core.design.image.ProvideAppImageLoader
import com.lyf.small.core.design.theme.AppTheme

/** 应用 UI 组合根；主题、图片能力和顶层导航在这里完成装配。 */
@Composable
fun SmallApp(imageLoader: ImageLoader) {
    AppTheme {
        ProvideAppImageLoader(imageLoader = imageLoader) {
            AppRootNavigation()
        }
    }
}
