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

- 依赖方向只允许 `app → {core:common, core:data, core:design, feature:*}`；
  feature 只依赖三个 core 模块。core 内部只允许 `core:data / core:design → core:common` 单向，
  禁止 core → app/feature（反向依赖）、feature 互相依赖、core:common 依赖任何兄弟模块。
- 底部 tab 的注册点是 `app/navigation/TopLevelTab.kt`（枚举持路由）+ `AppNavigation.kt` 的
  bottomBar；多返回栈机制在 `core:design/navigation/TabNavigation.kt`，切 tab 不清栈，
  各 tab 返回历史独立。登录等全局全屏流程走 `AppNavigation.kt` 的根栈，不塞进任一 tab 栈。
- 跨 feature 跳转只能在 `app/navigation/AppNavigation.kt` 用回调连接，feature 之间不互引页面。
- 新增数据库 Entity：Entity/Dao 放对应 feature 的 `data/local`，但必须到 `app` 的
  `AppDatabase` 注册；Room 的 KSP 处理器只挂在 `app/build.gradle.kts`，schema 导出在
  `app/schemas/`。feature 模块内无法独立构造 DAO（DAO 实现类只在 `:app` 生成），属已知取舍。
- 改数据库结构 = 新版本号 + 提交 `app/schemas/` 下新 JSON + 写迁移，三件事一起做。
- Retrofit 接口放 feature 的 `data/remote`；网络客户端与错误边界复用 `core:data` 的
  `NetworkFactory` / `NetworkResult`。

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
- 金额一律用 `core:data/model/Money`（最小货币单位 Long），展示用 `formatMoney`，禁止浮点。
- 日志走 `core:common/log/AppLogger`，不直接依赖 Kermit；网络错误走 `NetworkResult` 边界，
  `CancellationException` 必须原样重抛。
- 键值存储只注入 `core:data/storage/KeyValueStore` 接口，key 用业务模块的常量对象集中声明，
  不在调用点写裸字符串；MMKV 是 Android native 实现，JVM 单测跑不了真实现，测试用内存 Fake。
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
