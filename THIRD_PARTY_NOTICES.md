# 第三方组件说明

本项目直接依赖的主要第三方组件如下。此文件是工程清单，不代替正式法律意见；发布前应根据最终依赖锁定结果生成完整 notices 并保留许可证原文。

| 组件 | 用途 | 主要许可证 |
| --- | --- | --- |
| Kotlin / kotlinx.coroutines / kotlinx.serialization | 语言与异步/序列化基础 | Apache-2.0 |
| Android Gradle Plugin / Android SDK（AndroidX 全系） | 构建与运行时基础 | AGP: Apache-2.0；Android SDK 受 Android SDK License 约束 |
| Jetpack Compose（BOM/UI/Material3/Material Icons） | 声明式 UI | Apache-2.0 |
| AndroidX Lifecycle / Navigation 3 / Activity / Core Ktx / Splashscreen / Room | 生命周期、导航、数据库 | Apache-2.0 |
| Dagger Hilt（含 hilt-navigation-compose） | 依赖注入 | Apache-2.0 |
| Retrofit（含 converter-kotlinx-serialization） | 声明式 HTTP 客户端 | Apache-2.0 |
| AndroidX Media3（ExoPlayer / Compose UI / HLS / OkHttp DataSource / Database） | Feed 音乐与 MV 播放、缓存 | Apache-2.0 |
| OkHttp | 网络传输 | Apache-2.0 |
| MMKV（com.tencent:mmkv） | 键值存储 | BSD 3-Clause |
| Coil | 图片加载 | Apache-2.0 |
| Kermit | 日志门面 | Apache-2.0 |
| detekt（含 detekt-formatting，仅构建期） | 静态检查 | Apache-2.0 |
| JUnit 4 / Google Truth / androidx.test（仅测试） | 单元测试与基准配置生成 | Apache-2.0（JUnit: EPL-1.0） |

依赖升级或新增 SDK 时必须重新核对许可证、传递依赖和商店披露要求。
