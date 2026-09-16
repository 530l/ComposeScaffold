# Small

`Small` 是一个面向个人开发的单模块 Android Compose 基础工程。当前只有 `:app` 一个 Gradle 模块，源码按未来组件边界组织；项目变大后，可将对应目录平移为独立模块，而不需要先重命名包。

## 当前能力

- Hilt 应用组合根与协程调度器注入。
- Retrofit、OkHttp、kotlinx.serialization，网络响应统一由 Sandwich `ApiResponse` 表达（见 [docs/sandwich.md](docs/sandwich.md)）。
- 认证客户端和公共无认证客户端隔离，会话失效状态可重放。
- MMKV 非敏感键值存储与 Android Keystore 安全凭据存储。
- 统一日志、Miuix 主题和 Coil 图片入口。
- 页面采用裸 ViewModel + 单一 state class + 公开方法直调，一次性提示走独立事件流。
- AndroidX Navigation 3 四 Tab 应用壳：探索、AI 创作、资产、我的。
- 每个 Tab 持有独立返回栈，并保留 saveable 状态与 Entry 级 ViewModel 生命周期。
- Miuix `Scaffold`、`SmallTopAppBar`、`NavigationBar`、`Card` 和 `Text` 基础组件。
- 探索页接入 WanAndroid 首页轮播与文章分页，支持下拉刷新和触底加载。
- 探索页由单一 ViewModel 协调初始化、刷新、分页和部分失败状态，不额外引入 Controller 或 UseCase。

## 目录边界

```text
app/src/main/kotlin/com/lyf/small/
├── app/                  Application、Activity、组合根与应用级 DI
├── core/
│   ├── common/          配置、日志等通用能力
│   ├── data/            网络、存储、调度器与数据层 DI
│   └── design/          Miuix 主题、图片、导航与可复用 UI 组件
├── data/
│   └── content/         文章与轮播的数据契约、模型、API、DTO、映射和 DI
└── feature/
    ├── explore/         探索页路由、页面状态和 UI
    ├── creation/        AI 创作页面与路由
    ├── assets/          资产页面与路由
    └── mine/            我的页面与路由
```

依赖方向固定为 `app → feature/data/core`、`feature → data/core`、`data → core`。`core` 不反向依赖 `data`、`feature` 或 `app`，Feature 之间不直接依赖。某个目录职责稳定且构建成本开始明显增长时，再拆为同名 Gradle 模块。

Feature 只向 `app/navigation` 暴露 `NavKey`、`SerializersModule` 和 `EntryProvider`。跨 Feature 跳转继续由应用组合层连接，不在 Feature 内直接持有其他 Feature 的路由。

多个 Feature 可以复用的业务数据按领域放在顶层 `data/`，不归属于任何页面。目前 `data/content` 对外提供 `ContentRepository`、`Article`、`ArticlePage` 和 `Banner`；WanAndroid API、DTO、Mapper 与 Repository 实现保持 `internal`。以后新增账号、创作和资产数据时，分别建立 `data/account`、`data/creation` 和 `data/asset`，不要建立以页面命名的 `data/explore`。

网络失败的全链路货币是 Sandwich 的 `ApiResponse`：Retrofit 接口直接返回 `ApiResponse<信封>`，Repository 用 `unwrap()` 拍平信封并以 `mapSuccess` 映射为领域模型，失败以值表达、永不抛异常。`ApiCodes` 与 `ApiResponseExt`（断网/超时判定）位于 `core/data/network`；信封业务码经 `data/content` 的 `envelopeErrorCode()` 提取，页面不接触 DTO。有业务状态的页面使用裸 ViewModel：单一不可变 UiState 经 `update { copy() }` 原子更新，UI 通过公开方法（如 `refresh`/`retryInitial`/`loadMore`）直接驱动；一次性 Event 只承载允许在后台丢失的提示或导航效果，必须持续成立的信息放入 UiState。静态页面不创建无意义的 ViewModel。Feature 的 Route 持有 ViewModel，Content 和区域组件只接收状态与回调；分页、网络错误映射等业务规则仍留在对应 Feature，不进入通用基类。

Explore Feature 只保留导航与展示层。初始化、刷新、分页互斥、去重和错误分层由 `ExploreViewModel` 直接协调，当前规模不增加 Controller、UseCase 或 StateStore。

## UI 与导航版本

工程沿用 AGP `9.3.2` 的 Kotlin `2.4.20` 工具链，Miuix 使用 `top.yukonga.miuix.kmp:miuix-android:0.7.1`。页面优先使用 Miuix 的 `Scaffold`、`SmallTopAppBar`、`PullToRefresh`、`Card`、`Text`、`Button`、`InfiniteProgressIndicator` 和内置图标。探索页通过 `AppPullToRefresh` 使用 Miuix 原生下拉手势、状态机、内容位移和刷新动画；指示器使用 Miuix 主题主色，刷新状态使用中文资源文案与 Miuix `footnote1` 字体样式。Miuix `0.7.1` 没有可直接使用的 Snackbar Composable，因此刷新提示在 `Scaffold.snackbarHost` 内使用 Miuix `Card` 组合。轮播指示器使用 `com.tbuonomo:dotsindicator:5.1.0`。导航继续使用 AndroidX Navigation 3 `1.1.7`，未引入 Miuix 自带导航实现。

顶层 Tab 根路由分别位于各自的 `feature/*/navigation` 目录，命名为顶层 `data object XxxRoute`；对应入口页面的文件和 Composable 统一命名为 `XxxRouteScene`。向某个 Tab 增加详情页时，沿用 `XxxDetailRoute` 与 `XxxDetailRouteScene` 的组合，并在序列化模块和 EntryProvider 中注册。

## 环境配置

默认 `API_BASE_URL` 为 `https://wanandroid.com/`，探索页通过公开接口读取 `/banner/json` 和 `/article/list/{page}/json`。公开请求使用无认证 Retrofit，不发送业务 Token；密钥和 Token 不得写入 BuildConfig 或源码。

`KeyValueStore` 的 MMKV 实现只保存非敏感偏好和缓存标记。Token、刷新令牌等凭据必须使用 `SecureCredentialStore`；凭据操作失败会抛出 `CredentialStorageException`，调用方不得将失败当作保存成功。

## 文档

- [Sandwich 网络层集成](docs/sandwich.md)：依赖接入、信封拍平、错误分类、并发模式与已知边界。

## Release 优化

Release 使用 AGP `9.3+` 的 `optimization.enable = true`，同时启用 R8 代码优化和资源压缩。应用规则位于 `app/src/main/keepRules/rules.keep`，仅保留需要动态代理的业务 Retrofit API；Retrofit、Hilt、MMKV 和 Coil 等依赖继续使用各自发布的 consumer rules，不通过全包 `-keep` 降低优化效果。

R8 的依赖解析、混淆结果、Release 启动和真实网络解析必须在发布前通过 Release 构建与设备验证；仅存在规则文件不代表混淆行为已经验证。

## 独立运行

从本目录执行 Gradle 命令：

```bash
cd small
./gradlew :app:assembleDebug --console=plain
```

当前没有测试源码。根仓库的原多模块工程与本工程互不依赖，也不会自动构建本工程。
