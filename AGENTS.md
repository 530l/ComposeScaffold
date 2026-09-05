# AGENTS.md — ComposeScaffold 工程 Agent 工作守则

面向 Android 原生的商业化 Compose 脚手架（AGP 9.3.2 built-in Kotlin 2.2.10 / Compose BOM 2026.08.00 / compileSdk 37 / minSdk 24）。
姐妹项目是 CMP 版 `CmpAppScaffold`（KMP/iOS），本工程是独立的 Android-only 演进线，不与鸿蒙端对标。
模块边界、分层职责的完整说明见 `README.md`，本文件只补充 agent 操作层面的规则。

## 验证命令（改动代码后必须全绿才算完成）

```bash
./gradlew assembleDebug detekt testDebugUnitTest --console=plain
```

- detekt 配置在根 `config/detekt/detekt.yml`（buildUponDefaultConfig），各模块挂了
  detekt-formatting；放宽规则优先改 yml，其次才是改代码，禁止为过检删测试。
- Gradle daemon toolchain 是 JDK 22（`gradle/gradle-daemon-jvm.properties` + foojay 自动装）。
  **不要升到 23+**：detekt 1.23.8 内嵌的 Kotlin 编译器在 JDK 24+ 上直接崩
  （`IllegalArgumentException: 25.0.2`），22 是其支持上限。
- 不要用 `| tail` 管道包住 gradlew 判断成败，退出码会被吞。

## 模块与依赖铁律

- 依赖方向只允许 `app → {core:common, core:model, core:data, core:design, feature:*}`；
  feature 按需依赖四个 core 模块。`core:model` 为纯领域模型叶子底座（零项目依赖）；
  `core:data / core:design` 允许单向依赖 `core:common` 与 `core:model`；
  禁止 core → app/feature（反向依赖）、feature 互相依赖、core:model 依赖任何兄弟模块。
- 底部 tab 的注册点是 `app/navigation/TopLevelTab.kt`（枚举持路由）+ `AppNavigation.kt` 的
  bottomBar；多返回栈机制在 `core:design/navigation/TabNavigation.kt`，切 tab 不清栈，
  各 tab 返回历史独立。登录等全局全屏流程走 `AppNavigation.kt` 的根栈，不塞进任一 tab 栈。
- 跨 feature 跳转只能在 `app/navigation/AppNavigation.kt` 用回调连接，feature 之间不互引页面。
- 新增数据库 Entity：Entity/Dao 统一放入 `core:data` 对应的业务仓储包（如 `core/data/<domain>/local`），必须到 `app` 的
  `AppDatabase` 注册；Room 的 KSP 处理器只挂在 `app/build.gradle.kts`，schema 导出在 `app/schemas/`。
- 改数据库结构 = 新版本号 + 提交 `app/schemas/` 下新 JSON + 写迁移，三件事一起做。
- 数据模型与状态职责强隔离：
  - `core:model` 统一存放 API/服务端返回的数据模型（Data Model，如 `core.model.article.Article`）及全局基础值对象（如 `Money`, `NetworkResult`），严禁存放任何与 UI/交互相关的瞬态状态；
  - 各 Feature 独有的界面交互状态（如 `CartUiState`、`CartItemUiState` 包含的选中状态、折叠状态、输入草稿等）严格保留在各自 Feature 模块内，通过组合（Composition）方式按需包装 `core:model` 的数据实体，严禁在 Feature 中复制冗余的 DTO 或编写无意义的字段映射。
- 仓储与网络接口规范：Retrofit 接口统一扁平放入 `core:data/api`（按模块前缀命名，如 `CartApi.kt`），Repository 契约与实现统一扁平放入 `core:data/repository`（按模块前缀命名，如 `CartRepository.kt`），不分子包。Feature 为纯展示层，ViewModel 直接注入 Repository 并消费业务 Model，免除机械透传 UseCase。网络客户端复用 `core:data` 的 `NetworkFactory`，网络错误边界统一走 `core:model` 的 `NetworkResult`。

## 代码约定

- presentation 层 MVI 单向数据流：不可变 `UiState`（派生量用计算属性）+ sealed `Intent` +
  `onIntent()` 唯一入口；Composable 子组件只收状态与回调，不持有 ViewModel。
- 初始加载放 ViewModel `init {}`（列表页即 `loadable.initialize()`）。Nav3 entry 首次进
  组合才创建 VM，`init` 等价「首次进入屏幕」；切 tab/返回只是重进组合、VM 不重建，初始
  加载不会重跑，状态天然保留，单测构造 VM 后 advanceUntilIdle 即完成初始加载。
  Composable 禁止 `LaunchedEffect(Unit) { vm.loadXxx() }` 式 UI 直接触发业务加载，
  `LaunchedEffect` 只用于生命周期信号（返回键、权限申请）与事件收集（ObserveAsEvents）；
  刷新/重试/触底一律走 Intent，不引入 ScreenStarted 式启动 Intent。
- 分页列表统一走 `core:design` 的 `core/ui/loadmore`：UiState 实现 `LoadableUiState`，
  `LoadableController` 负责页码、互斥去重与结束判定（`Page(items, hasMore)` 由调用方按
  后端 cursor/总数信号显式给出，不要用「返回条数 < pageSize」推断）。
- 金额一律用 `core:model/Money`（最小货币单位 Long），展示用 `formatMoney`，禁止浮点。
- 日志走 `core:common/log/AppLogger`，不直接依赖 Kermit；网络错误走 `NetworkResult` 边界，
  `CancellationException` 必须原样重抛。
- 协程调度器一律注入 `@IoDispatcher` / `@DefaultDispatcher`（定义于 `core:data/coroutine`），
  禁止在数据层、领域层硬编码使用裸 `Dispatchers.IO` / `Dispatchers.Default`，单测构造时通过参数传入 `StandardTestDispatcher`。
- 键值与凭证存储强边界隔离：
  - 非敏感偏好设置、缓存标记只注入 `core:data/storage/KeyValueStore` 接口（MMKV 实现）；
  - Token、刷新令牌、密码和个人敏感信息一律注入 `core:data/storage/SecureCredentialStore` 接口
    （Android Keystore AES-256 GCM 硬件加密实现），严禁明文存入 MMKV。
- 网络认证与 401 登出链路：网络客户端由 `AuthInterceptor` 自动装配 `SecureCredentialStore` 的
  Bearer Token；遇到 401 响应通过 `SessionEventManager` 广播，应用根导航集中监听并重定向至全屏登录页，
  Feature 无需重复编写 401 拦截弹窗逻辑。
- 领域层 UseCase 约定：简单单表或单接口操作，保持 ViewModel 直接调用 Repository，
  禁止机械化堆叠仅有一行转发的透传式 UseCase（避免过度工程化）；仅在存在跨 Repository 聚合、
  复用率高或包含核心商业计算规则时才抽取单职责 UseCase（遵循单一 `operator fun invoke`）。
- Nav3 路由 `data object` 必须覆写 `toString()` 返回 `接口名.对象名`（如 `"CartRoute.Main"`）：
  导航宿主显式用 `key.toString()` 作 contentKey，是 saveable 状态（含滚动位置）与 entry 级
  ViewModelStore 的存取键；裸 `data object Main` 跨 feature 全叫 "Main"，会互相覆盖、
  弹出时互相误删（症状：返回/切 tab 后列表回顶部）。
- 多返回栈 tab 必须走 `core:design` 的 `TabAppNavHost`：每个 tab 的栈各自调用
  `rememberDecoratedNavEntries`，并持有各自独立的 entry decorators，NavDisplay 按 entries 切换。
  不要把不同栈轮流塞给同一个 NavDisplay 的 backStack 参数——上一 tab 的 entry 会被
  误判为弹出并清掉状态。
- 应用窗口保持 edge-to-edge，根导航不统一添加 safeDrawing padding；页面背景铺满窗口，
  文字、按钮等交互内容由页面自己的 Scaffold/TopAppBar/WindowInsets 避让系统栏和刘海。

## 版本与依赖

- 版本号以 `gradle/libs.versions.toml` 为唯一来源。新增/升级依赖先到
  repo1.maven.org 或 dl.google.com 的 `maven-metadata.xml` 核实最新版，
  **search.maven.org 的 latestVersion 会滞后，不可信**。默认选稳定版，不为追新上 alpha/beta。
- **AGP 9 built-in Kotlin = 2.2.10 元数据兼容铁律**：AGP 9 禁用 `org.jetbrains.kotlin.android`，
  Kotlin 版本由 AGP 内置通道锁死，**禁止升级 Kotlin 编译器插件版本，也禁止引入用
  Kotlin 2.3+ 编译的三方库**（其 metadata 无法被 2.2.10 消费，编译期报
  "metadata version is not supported"）。引入前先核实该库的 Kotlin 兼容性（查发布说明或
  module metadata）；stdlib 已由根 `build.gradle.kts` 的 resolutionStrategy 钉在 2.2.10，
  不要动。参考：kotlinx-serialization 用 1.9.0、Coil 用 3.5.0，均为元数据兼容版本。
- KSP 用独立版本号（当前 2.3.11），不跟 Kotlin 版本前缀绑定。
- Hilt 版本升级要同时核对内置 Kotlin 与 KSP 兼容矩阵。
- detekt 1.23.8 与 JDK daemon 22 的约束见上文「验证命令」。

## Git

- agent 代跑的提交署名 `git -c user.name="local-snapshot" -c user.email="snapshot@local"`，
  用户手动提交用本人全局身份。
- 构建产物、`local.properties`、keystore、`.env*` 均不入库（`.gitignore` 已覆盖）。
- release 签名参数走用户级 `~/.gradle/gradle.properties` 注入，绝不硬编码进仓库。
