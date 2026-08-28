package com.lyf.composescaffold.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test

/**
 * 启动基线配置生成器。当前 baselineprofile 插件尚未适配 AGP 9，此类暂不参与构建；
 * 恢复插件和模块依赖后，再通过对应的 generateBaselineProfile 任务生成配置文件。
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
