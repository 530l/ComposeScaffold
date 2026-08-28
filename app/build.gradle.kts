plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    // androidx.baselineprofile 插件尚未适配 AGP 9 新模块模型（issuetracker 443311090），
    // 适配版本发布后取消下一行注释并恢复下方 baselineProfile(project(...)) 依赖：
    // alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.lyf.composescaffold"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lyf.composescaffold"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 环境注入：debug/release 各自 baseUrl，AppConfig 会强制 HTTPS + 尾斜杠
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://www.wanandroid.com/\"",
        )
    }

    val releaseStoreFile = providers.gradleProperty("COMPOSE_SCAFFOLD_STORE_FILE")
    val releaseStorePassword = providers.gradleProperty("COMPOSE_SCAFFOLD_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("COMPOSE_SCAFFOLD_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("COMPOSE_SCAFFOLD_KEY_PASSWORD")
    val releaseSigningProps = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    )
    // 全有或全无：部分提供即配置期失败；全部缺省时 release 保持未签名，
    // 供本地 R8 验证。
    val hasAllSigningProps = releaseSigningProps.all { it.isPresent }
    if (hasAllSigningProps) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    } else if (releaseSigningProps.any { it.isPresent }) {
        error(
            "release 签名属性必须四项同时提供于 ~/.gradle/gradle.properties: " +
                "COMPOSE_SCAFFOLD_STORE_FILE/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD",
        )
    } else {
        logger.lifecycle("未配置 release 签名，release 构建将保持未签名（仅供本地验证）")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://www.wanandroid.com/\"",
            )
        }
        release {
            optimization {
                enable = true
            }
            if (hasAllSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    detektPlugins(libs.detekt.formatting)
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:design"))
    implementation(project(":feature:home"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:message"))
    implementation(project(":feature:cart"))
    implementation(project(":feature:login"))
    implementation(project(":feature:mine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(libs.androidx.profileinstaller)
    // baselineProfile(project(":baselineprofile")) // 待 baselineprofile 插件适配 AGP 9 后恢复

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
