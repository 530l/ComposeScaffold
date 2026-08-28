# ComposeScaffold

面向 Android 原生的商用 Compose 脚手架（姐妹项目：CMP 版 [CmpAppScaffold](../CmpAppScaffold)，本工程与其共享架构理念但独立演进）。应用壳提供首页、逛、消息、购物车、我的五个独立返回栈，登录等全局流程由根导航全屏覆盖。购物车示例接入了 wanandroid 文章分页接口（`article/list/{page}/json`），演示下拉刷新、触底加载、勾选与结算底栏的完整 MVI 链路。

## 技术栈

版本以 `gradle/libs.versions.toml` 为唯一来源，当前主要组件：

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Android Gradle Plugin | 9.3.2 | built-in Kotlin = **2.2.10**（AGP 9 禁用外部 KGP，stdlib 被钉死在 2.2.10） |
| KSP | 2.3.11 | 独立版本号，需 ≥2.3.1 才兼容 AGP 9 |
| Compose BOM | 2026.08.00 | Material 3 + AppCompat 主题 |
| Activity Compose / Core Ktx / Splashscreen | 1.13.0 / 1.19.0 / 1.2.0 | |
| Lifecycle | 2.11.0 | runtime-compose + viewmodel-compose + viewmodel-navigation3 |
| Navigation 3 | 1.1.7 | androidx `navigation3-ui`，多返回栈 Tab 容器在 `core:design` |
| Hilt | 2.60.1 | hilt-navigation-compose 1.4.0 |
| Retrofit | 3.0.0 | converter-kotlinx-serialization |
| OkHttp | 5.5.0 | 含 logging-interceptor |
| kotlinx-serialization / coroutines | 1.9.0 / 1.11.0 | 元数据兼容内置 Kotlin 2.2.10 的版本（勿盲目升级） |
| Room | 2.8.4 | schema 导出至 `app/schemas/` |
| MMKV | 2.4.2 | 键值存储（`KeyValueStore` 接口封装） |
| Coil | 3.5.0 | coil-compose + coil-network-okhttp（3.6.0 为 Kotlin 2.4 元数据，不可用） |
| Kermit | 2.1.0 | 统一日志门面（业务不直接依赖） |
| detekt | 1.23.8 | 静态检查 + detekt-formatting |
| 测试 | JUnit 4.13.2 / Truth 1.4.5 / Turbine 1.2.1 / Robolectric 4.16.1 | |

compileSdk 37 / targetSdk 37 / minSdk 24；JVM 工具链：Gradle daemon JDK 22（foojay 自动装）。

## 模块结构

```text
app                         应用壳：五 Tab 壳、根导航、初始化、DI/Room 数据库聚合、发布配置
  ├── core:common           纯 Kotlin 底座：日志（AppLogger）、运行配置（AppConfig），零 Compose 依赖
  ├── core:data             通用模型（Money/NetworkResult）、Retrofit/OkHttp 工厂、
  │                         KeyValueStore 接口与 MMKV 实现、核心 DI 模块
  ├── core:design           Compose 工具箱：主题、图片（AppImage/Coil）、刷新/加载更多组件族
  │                         （LoadableLazyColumn/LoadableController）、状态页与 Navigation 3 容器
  ├── feature:home          「首页」独立 tab 与 EntryProvider
  ├── feature:browse        「逛」独立 tab 与 EntryProvider
  ├── feature:message       「消息」独立 tab 与 EntryProvider
  ├── feature:cart          「购物车」data / domain / presentation / EntryProvider（wanandroid 分页示例）
  ├── feature:mine          「我的」独立 tab 与 EntryProvider
  └── feature:login         根级全屏登录骨架、路由与 EntryProvider
```

- 依赖方向：`app → core/feature`；feature 只依赖三个 core；core 内 `data/design → common` 单向；feature 之间、core → feature 反向依赖均禁止。
- presentation 层 MVI：不可变 `UiState` + sealed `Intent` + `onIntent()` 唯一入口；初始加载在 ViewModel `init {}`，Composable 不直接触发业务加载。
- 分页列表走 `core:design` 的 `LoadableController` 状态机 + `LoadableLazyColumn` 容器，互斥去重与结束判定有 JVM 单测覆盖。
- Room 数据库与 KSP 处理器集中在 `app`；各业务 Entity/Dao 在 feature 的 `data/local`，由 `app` 的 `AppDatabase` 注册。

## 环境配置

API 基地址通过 `app/build.gradle.kts` 的 `buildConfigField("String", "API_BASE_URL", ...)` 注入（debug/release 各自可配，`AppConfig` 会强制 HTTPS + 尾斜杠）。默认指向公开演示服务 `https://www.wanandroid.com/`，正式项目替换为自己的环境值。

Android release 签名通过用户级 `~/.gradle/gradle.properties` 注入；四项必须同时提供（全有或全无，缺一在配置阶段明确失败）：

```properties
COMPOSE_SCAFFOLD_STORE_FILE=/absolute/path/to/release.keystore
COMPOSE_SCAFFOLD_STORE_PASSWORD=replace_me
COMPOSE_SCAFFOLD_KEY_ALIAS=replace_me
COMPOSE_SCAFFOLD_KEY_PASSWORD=replace_me
```

不要把 API token、证书密码、签名私钥或真实生产密钥提交到仓库。

## 数据库升级约定

1. 修改 Entity 后递增 `AppDatabase.version`。
2. 提交 `app/schemas/` 生成的新 schema JSON。
3. 提供显式 migration 并覆盖升级测试；生产环境禁止 destructive migration。

## 本地运行与验证

```bash
# 完整验证链路（CI 同款）
./gradlew assembleDebug detekt testDebugUnitTest --console=plain
```

CI 在 `.github/workflows/ci.yml`：push/PR 触发，JDK 22（Temurin）+ Gradle 缓存，跑上面同款命令。

## 商用前仍需补齐

- 替换 applicationId、图标、品牌主题和演示 API 地址（wanandroid）。
- UI 测试（androidTest 目前仅模板用例）与更高覆盖的集成测试。
- 依赖漏洞扫描（如 Dependabot / dependency-check）与许可证合规流水线。
- 崩溃上报、性能监控、埋点、用户协议、隐私政策与账号注销流程。
- Android 数据安全表单、商店隐私声明与权限最小化审查。
- R8 混淆规则、签名发布流水线与渠道打包方案。
- 按发布地区完成第三方许可证、税务、支付、无障碍和合规审查。

第三方组件及许可证摘要见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
