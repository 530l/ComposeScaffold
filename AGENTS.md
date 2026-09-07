# AGENTS.md — ComposeScaffold 协作规则

本文件适用于整个仓库。目标是在保持 Android Compose 脚手架简洁的前提下，完成可审查、范围明确的改动。

## 1. 工作方式与验证命令

- 默认使用中文解释、总结和编写代码注释。
- 代码标识符、命令、路径、接口字段、日志与报错原文保持原样。
- 先阅读相关实现、调用方和现有测试，再修改代码；不要只依据 README 或本文件推断实现。
- 用户在当前会话中的明确要求优先于本文件；子目录 AGENTS.md 在其目录内补充或覆盖根目录规则，但不得覆盖更高优先级指令。
- 默认不执行 Test Plan，不运行构建、Detekt、单元测试、设备测试或自动格式化。改动后仅执行以下验证命令：

```bash
git diff --check
```

- 上述命令仅检查差异中的空白等问题，不证明代码能编译或功能正确。
- 收尾说明修改内容、实际执行的验证及未验证事项，禁止把未执行的检查写成通过。
- 只有用户明确要求扩大验证范围时，才运行对应命令；无需对已经授权的检查重复请示。
- 同一错误连续出现两次后，停止重复尝试；联网检索 3–5 种可能方案，优先核对官方资料，再选择符合当前授权和工具链约束的方案。不得上传凭据或敏感日志。
- 本次改动新增或修改了 `src/test` 下的测试文件时，运行对应模块的 `testDebugUnitTest` 属于已授权的最低验证，无需逐次请示。

以下为参考命令，不是默认执行指令：

```bash
# 当前 CI 的验证命令
./gradlew assembleDebug assembleRelease detekt testDebugUnitTest --console=plain

# 用户明确授权时，可按影响范围运行
./gradlew :feature:cart:testDebugUnitTest --console=plain
./gradlew detekt --console=plain
```

命令应在仓库根目录执行。不得使用会掩盖退出码的管道；以原始命令退出码判断成功与否。

## 2. 项目事实与信息来源

这是 Android 原生项目，不是 Kotlin Multiplatform 项目。不要因姐妹项目或遗留注释引入跨平台抽象。

当前为五 Tab 脚手架：

- 首页、逛、消息、购物车、我的各自持有返回栈。
- 登录是根级全屏占位流程，尚未实现真实认证。
- 购物车使用 wanandroid 文章分页演示选择、刷新、分页和金额展示，不是真实交易业务。

以以下配置作为事实来源：

- 模块注册：settings.gradle.kts。
- 依赖与插件版本：gradle/libs.versions.toml。
- Gradle Wrapper：gradle/wrapper/gradle-wrapper.properties。
- Daemon JDK：gradle/gradle-daemon-jvm.properties。
- 构建行为：根目录及各模块 build.gradle.kts。
- CI：.github/workflows/ci.yml。
- 格式与静态规则：.editorconfig、config/detekt/detekt.yml。

当前配置声明 AGP 9.3.2、Kotlin 插件版本 2.2.10、Compose BOM 2026.08.00；
compileSdk/targetSdk 为 37，minSdk 为 24。
Gradle Daemon 使用 JDK 22；Java 编译目标为 11，Detekt jvmTarget 为 21。
这些属于不同配置，不要混为一谈。

保持现有 built-in Kotlin 配置，不自行添加 org.jetbrains.kotlin.android 或 kapt。
版本目录中的 Kotlin 版本、stdlib 强制版本不能单独证明实际编译器版本。
不要把历史兼容问题写成永久结论，例如“JDK 22 是所有情况下的上限”。
依赖调整前核对实际工具链、插件和元数据兼容性，不盲目升级。

## 3. 模块职责与依赖方向

| 模块 | 职责 |
| --- | --- |
| app | 应用入口、五 Tab 壳、根导航、跨 Feature 回调连接、Hilt 组合根、Room 聚合、发布配置 |
| core:common | AppConfig、AppLogger 等无 UI 基础能力；当前是 Android library，依赖 Kermit |
| core:model | Article 等共享数据模型、Money、NetworkResult；当前使用 kotlinx.serialization，无 UI、网络客户端和数据库框架依赖 |
| core:data | API、Repository 契约与实现、网络、存储、调度器及数据层 DI |
| core:player | 通用媒体播放接口、Media3 实现、缓存、视频输出与系统会话 |
| core:design | 主题、图片入口、通用 Compose 组件、分页状态机和 Nav3 容器 |
| feature:* | 页面展示、页面状态、用户 Intent、ViewModel、路由及 EntryProvider |
| baselineprofile | 保留性能生成器源码；插件目前停用 |

依赖方向：

- app 按需依赖 feature 和 core。
- feature 按需依赖 core，禁止 Feature 之间直接依赖。
- core:data 和 core:design 可依赖 core:model、core:common，彼此保持独立。
- core:player 仅依赖 core:common；网络客户端由 app 注入，不依赖业务模型或数据层。
- core:model、core:common 不依赖其他项目模块。
- 禁止 core 反向依赖 app 或 feature。

core:data 的 api/、repository/、di/ 当前按业务前缀扁平组织，例如 CartApi、CartRepository。
先沿用现有结构；出现明确的职责或规模问题时再提出调整。

当前 AppDatabase 只有占位 Entity，没有业务 DAO。
新增业务 Entity/DAO 按数据层职责放入 core:data，由 app 聚合；届时同步处理所需依赖、处理器和迁移，不要依据旧注释放进 feature/data。

## 4. 代码与架构约定

### 状态与业务交互

- 有业务状态的页面沿用不可变 UiState、sealed Intent 和 onIntent() 入口。
- 静态占位页不必创建无意义的 ViewModel、Intent 或 UseCase。
- Screen 可获取 ViewModel、收集状态和处理导航事件；Content 与可复用组件接收状态及回调。
- 首次加载由 ViewModel 初始化负责；用户刷新、重试和翻页通过 Intent 进入。
- 不在 LaunchedEffect(Unit) 中直接调用业务加载。
- 允许使用 Compose 副作用收集事件、观察滚动或执行 UI 动画；不要一概禁止 LaunchedEffect。
- 状态使用 collectAsStateWithLifecycle；一次性界面事件沿用 ObserveAsEvents，并评估后台期间是否允许丢失。
- 必须在返回前台后继续成立的信息应表达为状态，不能仅依赖不重放的事件。
- ViewModel 生命周期由导航作用域决定；不要声称 init 能保证整个进程只加载一次。

真实参考：CartViewModel.kt、CartScreen.kt。

### 模型与数据层

- core:model 不放选中、展开、加载中等 UI 状态。
- 页面状态优先组合已有实体，不机械复制 DTO 字段。
- 允许为实际的业务语义转换、协议隔离或多数据源整合增加映射；禁止空转映射。
- 简单业务直接注入 Repository，不为单行转发创建 UseCase。
- API 协议解析、服务端错误和后端页码适配放在数据层；UI 不直接依赖 Retrofit 或调用网络接口。
- 页面内部类型优先使用 internal/private，只公开必要的跨模块接口。

组合示例：

```kotlin
internal data class CartItemUiState(
    val article: Article,
    val unitPrice: Money,
    val selected: Boolean = false,
)
```

### 协程与分页

- 需要指定 IO/CPU 调度器时，使用已有 @IoDispatcher、@DefaultDispatcher 注入。
- ViewModel 的业务协程使用 viewModelScope；不要在业务类中创建无生命周期的全局作用域。
- 保留 CancellationException 的取消语义，不转换为普通失败或成功。
- 复用 LoadableController 和 LoadableLazyColumn，不复制分页状态机。
- 控制器与 Repository 使用 1 基页码，CartRepository 转换为 wanandroid 的 0 基页码。
- 修改分页时共同检查列表、下一页页码、结束状态和进行中的请求。
- 刷新失败保留旧列表时，应同时保留与之匹配的分页状态。
- Lazy 列表 key 必须稳定且唯一；区分服务端空页与客户端去重后的空列表。
- 当前控制器仍存在刷新失败后结束状态丢失、去重空页提前结束的问题，不能把现有行为当成正确规范。

### 金额、UI 与导航

- 金额使用 Money，以 Long 最小货币单位存储，通过 formatMoney 展示。
- 保留负数、币种及溢出检查，不使用 Float/Double 计算金额。
- demoUnitPrice 仅用于演示，不能作为生产定价逻辑。
- UI 文案使用资源，样式优先使用 AppTheme/MaterialTheme；保持无障碍语义和系统 Insets 处理。
- Hilt ViewModel 使用 androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel。
- 路由实现 NavKey、支持序列化，并注册对应 SerializersModule 和 EntryProvider。
- 当前导航容器显式使用 key.toString() 作为 contentKey，因此 data object 路由必须返回带命名空间的稳定名称，例如 "CartRoute.Main"。
- 参数化路由还要评估参数和重复入栈的身份需求，不能统一返回固定名称。
- 跨 Feature 跳转由 app 连接回调，Feature 不直接操作其他 Feature 的返回栈。
- 修改导航时保留各 Tab 独立返回栈、状态恢复和 Entry 级 ViewModel 生命周期。

### 风格

- 遵循 .editorconfig 和相邻代码风格，Kotlin 使用 4 空格及尾随逗号。
- 注释说明原因、约束和边界，使用中文，避免“商业级”“绝对安全”等无证据保证。
- 不为排版、命名偏好或等价写法扩大改动范围。
- 不复制当前文件中的死代码、演示逻辑或已知问题作为新代码模板。

## 5. 安全与修改边界

### 必须遵守

- KeyValueStore/MMKV 只保存非敏感偏好和缓存标记。
- 必要的敏感凭据通过 SecureCredentialStore 管理，禁止明文落盘。
- 不把 Android Keystore 自动等同于所有设备都有硬件保障。
- 不为方便 JVM 测试在生产实现中增加固定密钥或静默内存降级；测试使用显式替身。
- 凭据保存失败必须可被调用方识别；当前通过 CredentialStorageException 显式抛出实现。
- 认证凭据仅发往受信任 API 范围；公共图片请求不得自动携带业务 Token。
- 图片加载器与媒体数据源统一使用 @PublicHttpClient 公共客户端，不携带业务凭据。
- 日志统一使用 AppLogger，不输出 Token、密码、签名 URL、完整请求/响应正文或未经脱敏的异常信息。
- 保持 HTTPS、禁止明文流量等已有安全配置。

### 需要先确认

仅在当前任务未包含对应授权时确认：

- 添加或升级依赖、改变工具链。
- 修改 Room Entity/Schema。
- 修改顶层 Tab、根导航拓扑或公共返回栈机制。
- 修改签名、发布或生产安全配置。

先完成只读分析，说明具体改动和影响，再请求必要决策；已有授权不重复请示。

Room Schema 变更必须规划版本递增、导出的 schema JSON 和显式 Migration。
如当前验证授权不足以生成或验证 schema，应说明限制，不伪造生成结果。
禁止使用 destructive migration 掩盖升级问题。

### 禁止

- 为通过检查而删除、弱化或注释失败测试，或关闭相关规则。
- 提交 local.properties、.env*、keystore、签名密码、Token 和构建产物。
- 未经要求回滚、覆盖或清理用户已有改动。
- 把尚未执行的构建、测试、设备验证或安全保证写成已完成。
- 为形式统一进行全仓迁移、机械分层或批量重写。

## 6. Git 与交付

- 开始时检查 git status 和相关差异，明确已有改动。
- 用户要求 review 时先给发现、依据、影响和建议，不自动实施重构。
- 修改只覆盖当前任务；更新因本次改动而失真的相关文档。
- 不自动提交或推送；用户要求提交时，只暂存本次相关文件。
- Agent 创建提交时使用以下身份：

```bash
git -c user.name="local-snapshot" -c user.email="snapshot@local" commit -m "<type>: <description>"
```

提交类型使用 feat、fix、refactor、test、docs 等，描述具体结果。
不要为了满足“干净工作区”而删除或提交用户已有文件。

收尾报告：

- 完成了什么以及为什么。
- 实际执行的验证结果。
- 未验证的行为、已知限制或需要用户处理的事项。

本文件随代码演进更新。保留能防止实际错误的规则；详细教程放 README，
审查发现放任务报告，不持续堆积到本文件。
