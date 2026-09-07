# ComposeScaffold

面向 Android 原生的商用 Compose 脚手架（姐妹项目：CMP 版 [CmpAppScaffold](../CmpAppScaffold)，本工程与其共享架构理念但独立演进）。应用壳提供首页、逛、消息、购物车、我的五个独立返回栈，登录等全局流程由根导航全屏覆盖。购物车示例接入了 wanandroid 文章分页接口（`article/list/{page}/json`），演示下拉刷新、触底加载、勾选与结算底栏的完整 MVI 链路。

## 技术栈

版本以 `gradle/libs.versions.toml` 为唯一来源，当前主要组件：

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Android Gradle Plugin | 9.3.2 | built-in Kotlin = **2.2.10**（AGP 9 禁用外部 KGP，stdlib 被钉死在 2.2.10） |
| KSP | 2.3.11 | 独立版本号，需 ≥2.3.1 才兼容 AGP 9 |
| Compose BOM | 2026.08.00 | Compose UI + Material 3 |
| Activity Compose / Core Ktx / Splashscreen | 1.13.0 / 1.19.0 / 1.2.0 | |
| Lifecycle | 2.11.0 | runtime-compose + viewmodel-compose + viewmodel-navigation3 |
| Navigation 3 | 1.1.7 | androidx `navigation3-ui`，多返回栈 Tab 容器在 `core:design` |
| Hilt | 2.60.1 | hilt-navigation-compose 1.4.0 |
| Retrofit | 3.0.0 | converter-kotlinx-serialization |
| OkHttp | 5.5.0 | 仅记录脱敏后的开发环境请求摘要 |
| kotlinx-serialization / coroutines | 1.9.0 / 1.11.0 | 元数据兼容内置 Kotlin 2.2.10 的版本（勿盲目升级） |
| Room | 2.8.4 | schema 导出至 `app/schemas/` |
| MMKV | 2.4.2 | 非敏感键值存储（`KeyValueStore` 接口封装） |
| Coil | 3.5.0 | coil-compose + coil-network-okhttp（3.6.0 为 Kotlin 2.4 元数据，不可用） |
| Media3 | 1.11.0 | ExoPlayer / Compose UI / OkHttp DataSource / HLS / Database / Session，统一封装在 `core:player` |
| Kermit | 2.1.0 | 统一日志门面（业务不直接依赖） |
| detekt | 1.23.8 | 静态检查 + detekt-formatting |
| 测试 | JUnit 4.13.2 / Truth 1.4.5 / AndroidX Test | |

compileSdk 37 / targetSdk 37 / minSdk 24；JVM 工具链：Gradle daemon JDK 22（foojay 自动装）。

## 模块结构

```text
app                         应用壳：五 Tab 壳、根导航、初始化、DI/Room 数据库聚合、发布配置
  ├── core:common           基础层（Android library、零 UI 依赖）：日志（AppLogger）、运行配置（AppConfig）
  ├── core:model            数据实体底座（零外部框架/IO 依赖）：API 返回的业务数据模型（如 Article）及通用值对象（Money、NetworkResult）
  ├── core:data             统一数据层与基础设施：
  │                         - repository/（扁平存放各模块仓储契约与实现，用模块前缀区分，如 CartRepository.kt）
  │                         - api/（扁平存放 Retrofit 接口，用模块前缀区分，如 CartApi.kt）
  │                         - network/（网络工厂、Auth 拦截与 401 会话失效流）
  │                         - storage/（KeyValueStore / SecureCredentialStore 硬件加密）
  ├── core:player           可复用 Media3 封装：播放器接口、播放器池、缓存、视频输出与系统会话（见 core/player/README.md）
  ├── core:design           Compose 工具箱：主题、图片（AppImage/Coil）、刷新/加载更多组件族
  │                         （LoadableLazyColumn/LoadableController）、状态页与 Navigation 3 容器
  ├── feature:home          「首页」纯展示模块（独立 tab 与 EntryProvider）
  ├── feature:browse        「逛」音乐 / MV / 混合 Feed（Compose Pager、Media3、模拟分页）
  ├── feature:message       「消息」纯展示模块（独立 tab 与 EntryProvider）
  ├── feature:cart          「购物车」纯展示模块（MVI 单向流 + 列表状态机，直接注入 core:data 仓储并消费 core:model 模型）
  ├── feature:mine          「我的」纯展示模块（独立 tab 与 EntryProvider）
  └── feature:login         根级全屏登录骨架、路由与 EntryProvider
```

- 依赖方向：`app → core/feature`；feature 按需依赖 core 模块；core 内部 `core:model` 为零依赖纯叶子底座，`core:data / core:design` 单向依赖 `core:model` 与 `core:common`；`core:player` 仅依赖 `core:common`，不依赖 Feed 模型；禁止 core → app/feature（反向依赖）、feature 互相依赖。
- 模型与状态约定：
  - `core:model` 存放所有 API 服务端返回的数据模型及全局值对象；
  - 各 Feature 独有的界面交互状态（选中、展开、草稿等）保留在 Feature 内部的 `UiState` 中，通过「组合（Composition）」直接包裹 `core:model` 实体，免除冗余 DTO 与机械映射；
  - Feature 作为纯 Presentation 层（`presentation/` + `navigation/`），ViewModel 直接注入 `core:data` 的 Repository，免除机械透传 UseCase。
- presentation 层 MVI：不可变 `UiState` + sealed `Intent` + `onIntent()` 唯一入口；初始加载在 ViewModel `init {}`，Composable 不直接触发业务加载。
- 分页列表走 `core:design` 的 `LoadableController` 状态机 + `LoadableLazyColumn` 容器，互斥去重与结束判定有 JVM 单测覆盖。
- Room 数据库与 KSP 处理器集中在 `app`；各业务 Entity/Dao 集中在 `core:data`，由 `app` 的 `AppDatabase` 注册。

## 环境配置

API 基地址通过 `app/build.gradle.kts` 的 `buildConfigField("String", "API_BASE_URL", ...)` 注入（debug/release 各自可配，`AppConfig` 会强制 HTTPS + 尾斜杠）。默认指向公开演示服务 `https://www.wanandroid.com/`，正式项目替换为自己的环境值。

Android release 签名通过用户级 `~/.gradle/gradle.properties` 注入；四项必须同时提供（全有或全无，缺一在配置阶段明确失败）：

```properties
COMPOSE_SCAFFOLD_STORE_FILE=/absolute/path/to/release.keystore
COMPOSE_SCAFFOLD_STORE_PASSWORD=replace_me
COMPOSE_SCAFFOLD_KEY_ALIAS=replace_me
COMPOSE_SCAFFOLD_KEY_PASSWORD=replace_me
```

不要把 API token、证书密码、签名私钥或真实生产密钥提交到仓库。未配置上述四项时 release 产物保持未签名，只用于本地 R8 验证，不能发布。

`KeyValueStore` 的默认 MMKV 实现不加密，只能保存界面偏好、缓存标记等非敏感状态。登录 token、密码、支付凭证和个人敏感信息统一使用 `core:data` 的 `SecureCredentialStore`（基于 Android Keystore 硬件级 AES-256 GCM 加密，并内建 JVM 单测透明降级）。

## 数据库升级约定

1. 修改 Entity 后递增 `AppDatabase.version`。
2. 提交 `app/schemas/` 生成的新 schema JSON。
3. 提供显式 migration 并覆盖升级测试；生产环境禁止 destructive migration。

## 本地运行与验证

```bash
# 完整验证链路（CI 同款）
./gradlew assembleDebug detekt testDebugUnitTest --console=plain
```

CI 在 `.github/workflows/ci.yml`：push/PR 触发，JDK 22（Temurin）+ Gradle 缓存；除上述检查外还构建未签名 release，提前发现 R8 问题。

## 商用前仍需补齐

- 替换 applicationId、图标、品牌主题和演示 API 地址（wanandroid）。
- UI 测试（androidTest 目前没有业务场景）与更高覆盖的集成测试。
- baselineprofile 插件适配 AGP 9 后恢复生成任务，并用 Macrobenchmark 验证关键用户路径。
- 依赖漏洞扫描（如 Dependabot / dependency-check）与许可证合规流水线。
- 崩溃上报、性能监控、埋点、用户协议、隐私政策与账号注销流程。
- Android 数据安全表单、商店隐私声明与权限最小化审查。
- 按实际接入的业务 SDK 补齐 R8 规则，并建设签名发布流水线与渠道打包方案。
- 按发布地区完成第三方许可证、税务、支付、无障碍和合规审查。

「逛」已实现默认混合流，可切换纯音乐和纯 MV；保留五 Tab 导航。数据来源、播放器边界、本地采样配置和未执行的验收场景见 [Feed 实现说明](docs/feed.md)。

第三方组件及许可证摘要见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
