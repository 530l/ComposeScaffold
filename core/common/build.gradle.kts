plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.lyf.composescaffold.core.common"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    detektPlugins(libs.detekt.formatting)
    // core:common 是纯 Kotlin 底座：禁止依赖 Compose / 任何兄弟模块
    implementation(libs.kermit)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
