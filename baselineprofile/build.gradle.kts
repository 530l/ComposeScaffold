plugins {
    alias(libs.plugins.android.test)
    // androidx.baselineprofile 插件（1.4.1/1.5.0-rc 均在内）尚未适配 AGP 9 的新模块模型
    // （TestExtension 类型变更，issuetracker 443311090）。适配版本发布后：
    // 1) 此处恢复 alias(libs.plugins.baselineprofile)
    // 2) app/build.gradle.kts 恢复 baselineProfile(project(":baselineprofile")) 与插件行
}

android {
    namespace = "com.lyf.composescaffold.baselineprofile"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 24
        targetProjectPath = ":app"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// 当前仅保留生成器源码；恢复 baselineprofile 插件后才会出现对应生成任务。
dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.ext.junit)
}
