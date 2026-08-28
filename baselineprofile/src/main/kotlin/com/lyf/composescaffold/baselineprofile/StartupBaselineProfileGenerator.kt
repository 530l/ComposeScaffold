package com.lyf.composescaffold.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test

/**
 * 启动基线配置生成器：连接模拟器运行
 * `./gradlew :app:generateReleaseBaselineProfile`，
 * 产出的 profile 会在 release 构建中随 APK 分发（profileinstaller）。
 */
class StartupBaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect("com.lyf.composescaffold") {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            // 走查三个代表性 tab（首页/发现/购物车），覆盖主导航路径
            device.findObject(By.text("发现"))?.click()
            device.waitForIdle()
            device.findObject(By.text("购物车"))?.click()
            device.waitForIdle()
            device.findObject(By.text("首页"))?.click()
            device.waitForIdle()
        }
    }
}
