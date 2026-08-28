package com.lyf.composescaffold

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyf.composescaffold.core.design.AppTheme
import com.lyf.composescaffold.core.image.ProvideAppImageLoader
import com.lyf.composescaffold.navigation.AppNavigation
import okhttp3.OkHttpClient

/** 应用 UI 组合根：依赖注入在 MainActivity 完成，往下全部是平台无关代码。 */
@Composable
fun ScaffoldApp(okHttpClient: OkHttpClient) {
    AppTheme {
        ProvideAppImageLoader(okHttpClient = okHttpClient) {
            // 应用背景覆盖完整物理窗口，页面内容再按需消费系统安全区。
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AppNavigation()
            }
        }
    }
}
