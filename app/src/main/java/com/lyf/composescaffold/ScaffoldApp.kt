package com.lyf.composescaffold

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import com.lyf.composescaffold.core.data.network.SessionEventManager
import com.lyf.composescaffold.core.design.AppTheme
import com.lyf.composescaffold.core.design.image.ProvideAppImageLoader
import com.lyf.composescaffold.navigation.AppNavigation

/** 应用 UI 组合根：ImageLoader 为 Hilt 提供的进程单例，往下全部是平台无关代码。 */
@Composable
fun ScaffoldApp(
    imageLoader: ImageLoader,
    sessionEventManager: SessionEventManager? = null,
) {
    AppTheme {
        ProvideAppImageLoader(imageLoader = imageLoader) {
            // 应用背景覆盖完整物理窗口，页面内容再按需消费系统安全区。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AppNavigation(sessionEventManager = sessionEventManager)
            }
        }
    }
}
