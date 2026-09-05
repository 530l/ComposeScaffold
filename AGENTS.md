# AGENTS.md — ComposeScaffold 工程 Agent 工作守则

面向 Android 原生商用 Compose 脚手架（AGP 9.3.2 built-in Kotlin 2.2.10 / Compose BOM 2026.08.00 / compileSdk 37 / minSdk 24）的 AI 协同开发守则与操作规程。

---

## 1. 验证命令（Commands You Can Use — 必须首先掌握）

任何代码修改后，**必须运行并通过以下验证命令（退出码为 0 且 100% 绿灯才算完成任务）**：

```bash
# 全工程核心验证命令（构建 + Detekt 静态检查 + 单元测试，CI 同款）
./gradlew assembleDebug detekt testDebugUnitTest --console=plain
```

### 单模块/局部常用命令：
```bash
# 仅运行指定 Feature 的单元测试（例如 cart 模块）
./gradlew :feature:cart:testDebugUnitTest --console=plain

# 仅运行代码格式与静态检查
./gradlew detekt --console=plain

# 重新生成 Baseline Profile（需适配环境）
./gradlew :baselineprofile:assembleDebug --console=plain
```

### 工具链约束铁律：
- **Gradle Daemon JDK 必须为 22**（由 `gradle/gradle-daemon-jvm.properties` + foojay 自动拉取）：
  **严禁升至 JDK 23+**。Detekt 1.23.8 内置的 Kotlin 编译器在 JDK 24+ 上会发生崩溃（`IllegalArgumentException: 25.0.2`），JDK 22 是其支持上限。
- **禁止使用管道吞噬退出码**：
  严禁使用 `./gradlew ... | tail -n 20`，管道会吞掉非零退出码导致误判构建结果。

---

## 2. 行为边界准则（Three-Tier Boundaries）

基于 2500+ 代码仓库的最佳实践，Agent 在本工程中严格遵守以下三级操作边界：

### ✅ Always（必须做到）
1. **验证全绿**：改动任何 Kotlin/Gradle 文件后，必须执行全量验证命令且全绿。
2. **金额强类型**：涉及货币与金额一律使用 `core:model/Money`（以分/最小单位 Long 存储），展示格式化走 `formatMoney`，禁止浮点数计算。
3. **MVI 单向流**：Presentation 层严格遵循不可变 `UiState` + sealed `Intent` + `onIntent()` 单一入口；Composable 组件只接收状态和回调。
4. **初始加载内聚**：首次数据加载必须在 ViewModel `init {}` 中调用（如 `loadable.initialize()`），禁止由 UI 声明周期副作用拉起业务。
5. **调度器可注入**：协程调度器一律注入 `@IoDispatcher` / `@DefaultDispatcher`，单测中注入 `StandardTestDispatcher`。
6. **硬件级安全加密**：Token、密码、支付凭证统一注入 `core:data/storage/SecureCredentialStore`（基于 Android Keystore AES-256 GCM 硬件加密实现）。
7. **Nav3 路由命名**：Nav3 路由 `data object` 必须覆写 `toString()` 返回 `接口名.对象名`（如 `"CartRoute.Main"`），作为 contentKey 确保多返回栈状态保存。
8. **Git Agent 署名**：Agent 自动生成的快照提交必须署名 `git -c user.name="local-snapshot" -c user.email="snapshot@local"`。

### ⚠️ Ask First（修改前必须请示用户）
1. **添加/升级依赖**：修改 `gradle/libs.versions.toml` 引入新库或升版前，必须向用户确认并核实该库是否兼容 Kotlin 2.2.10 元数据（AGP 9 锁死内置 Kotlin 2.2.10，任何 2.3+ 编译的三方库均不可用）。
2. **修改数据库 Schema**：变更 Room Entity 结构前必须请示（需同时完成：递增版本号 + 导出新 JSON schema + 编写 Migration 迁移）。
3. **修改全局导航拓扑**：修改 `TopLevelTab.kt`、根导航 `AppNavigation.kt` 或公共返回栈机制。

### 🚫 Never（绝对禁止的红线）
1. **严禁删测试过检**：禁止为了让构建/Lint 通过而删除、弱化或注释掉失败的单元测试。
2. **严禁污染 core:model**：`core:model` 只存服务端返回的数据实体与通用值对象，**严禁放入任何 UI 状态**（如 `isSelected`, `isExpanded`, `isLoading`）。
3. **严禁在 Feature 中机械复制 DTO**：Feature 严禁自建与 API 结构一模一样的重复 Model 类或编写空转的 DTO 映射代码。
4. **严禁空转 UseCase**：简单单表/单接口 CRUD 严禁编写仅有一行调用的透传 UseCase（避免过度设计）。
5. **严禁跨 Feature 直接依赖**：依赖方向只能是 `app -> core/feature` 与 `feature -> core`。Feature 之间互不可见，禁止 Feature 互相依赖，禁止 Core 反向依赖 Feature。
6. **严禁明文存敏感数据**：MMKV（`KeyValueStore`）仅用于非敏感配置与缓存标记，严禁存入 Token 或用户凭证。
7. **严禁在 UI 中触发业务加载**：禁止使用 `LaunchedEffect(Unit) { viewModel.load() }` 直接调用业务请求。
8. **严禁提交敏感资产**：构建产物、`local.properties`、keystore、`.env*` 严禁提交进 Git。

---

## 3. 架构分层与目录规范（Architecture & File Structure）

```
app                         应用壳：五 Tab 壳、根导航、Room 数据库聚合、Hilt 根组件
  ├── core:common           通用底座（零 UI 依赖）：日志门面（AppLogger）、配置（AppConfig）
  ├── core:model            数据实体底座（零外部框架/IO 依赖）：
  │                         - API 服务端返回的业务模型（如 Article, WanApiResponse, ArticlePage）
  │                         - 基础领域值对象（Money, PriceUtils）
  │                         - 网络异常与结果模型（NetworkResult, NetworkError）
  ├── core:data             统一数据仓储与基础设施：
  │   ├── repository/       扁平存放各 Feature 的仓储契约与实现（模块前缀命名，如 CartRepository.kt）
  │   ├── api/              扁平存放 Retrofit 接口（模块前缀命名，如 CartApi.kt）
  │   ├── di/               Hilt 数据注入模块（如 CartDataModule.kt）
  │   ├── network/          网络基础设施（NetworkFactory, SafeRequest, AuthInterceptor, SessionEventManager）
  │   ├── storage/          存储抽象（KeyValueStore / SecureCredentialStore 硬件加密）
  │   └── coroutine/        调度器限定符（@IoDispatcher, @DefaultDispatcher）
  ├── core:design           Compose UI 工具箱：主题（AppTheme）、图片（AppImage/Coil）、Loadable 列表状态机、Nav3 容器
  └── feature:<name>        纯 Presentation 业务模块（feature:cart, feature:home 等）：
      ├── presentation/     MVI 界面交互（UiState + Intent + ViewModel + Composable 页面）
      └── navigation/       路由契约与 EntryProvider
```

---

## 4. 代码范式对照示例（Standards & Concrete Examples）

### 示例 1：模型与状态隔离（组合优于重复映射）

```kotlin
// ❌ 错误：在 Feature 中重复拷贝整套字段定义并写冗余 Mapper
data class CartArticleUiModel(val id: Long, val title: String, val author: String, val selected: Boolean)
fun Article.toUiModel() = CartArticleUiModel(id, title, author, false)

// ❌ 错误：把 UI 状态污染到 core:model 实体中
@Serializable
data class Article(val id: Long, val title: String, var isSelected: Boolean = false)

// ✅ 正确：core:model 保持纯净数据；Feature 的 UIState 通过组合直接包裹实体
// 在 feature:cart/presentation/CartViewModel.kt 中：
internal data class CartItemUiState(
    val article: Article,           // 直接复用 core:model 实体，零拷贝
    val unitPrice: Money,           // 业务派生状态
    val selected: Boolean = false,  // 页面私有的交互状态
)
```

### 示例 2：Hilt ViewModel 正确引入

```kotlin
// ❌ 错误：使用已废弃的旧路径（会导致编译器 deprecation 警告）
import androidx.hilt.navigation.compose.hiltViewModel

// ✅ 正确：使用 AndroidX 官方推荐的生命周期包
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
internal fun CartScreen(
    viewModel: CartViewModel = hiltViewModel(),
) { ... }
```

### 示例 3：初始加载与 MVI 单向流

```kotlin
// ❌ 错误：在 Composable 中使用 LaunchedEffect 触发首次数据加载（切 tab 重复触发）
@Composable
fun CartScreen(viewModel: CartViewModel) {
    LaunchedEffect(Unit) { viewModel.loadArticles() }
}

// ✅ 正确：在 ViewModel 的 init {} 块中初始化，状态在进程存活期自动保留
@HiltViewModel
internal class CartViewModel @Inject constructor(
    private val repository: CartRepository,
) : ViewModel() {
    private val loadable = LoadableController(..., loadPage = ::loadPage)

    init {
        loadable.initialize() // 首次进入进组合即触发，切 Tab 重进不会重复触发
    }

    fun onIntent(intent: CartIntent) { ... }
}
```

### 示例 4：金额计算与展示

```kotlin
// ❌ 错误：使用 Double / Float 计算商业金额（存在精度丢失风险）
val total: Double = items.sumOf { it.price * it.quantity }
val display = "¥${total}"

// ✅ 正确：统一使用 core:model 的 Money（单位：分）与 formatMoney
val total: Money = items.fold(Money.zero()) { acc, item -> acc + item.unitPrice }
val display: String = formatMoney(total) // 输出标准 ¥XX.XX
```

### 示例 5：Nav3 路由定义

```kotlin
// ❌ 错误：裸 data object 使用默认 toString()，会导致跨 feature contentKey 冲突并重置状态
@Serializable
data object Main : NavRoute

// ✅ 正确：必须覆写 toString() 包含前缀命名空间
@Serializable
data object Main : CartRoute {
    override fun toString(): String = "CartRoute.Main"
}
```

---

## 5. Git 提交与协作守则

- **Agent 提交命令**：
  ```bash
  git -c user.name="local-snapshot" -c user.email="snapshot@local" commit -m "<type>: <description>"
  ```
- **Commit Type 规范**：
  - `feat`: 新增业务功能或模块；
  - `fix`: 修复缺陷或错误；
  - `refactor`: 架构重构（如目录扁平化、下沉公共层）；
  - `test`: 新增或调整单元测试；
  - `docs`: 文档变更或工作守则更新。
- **干净工作区原则**：提交前确保无未追踪临时文件，构建产物严禁入库。
