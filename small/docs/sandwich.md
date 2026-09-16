# Sandwich 网络层集成

本工程的网络错误模型统一使用 [skydoves/Sandwich](https://github.com/skydoves/sandwich) `2.4.0`（`sandwich-bom` + `sandwich-retrofit`），`ApiResponse` 是从 Retrofit 接口到 ViewModel 的全链路货币。本文记录本工程的接入方式、约定与已知边界；API 细节以[官方文档](https://skydoves.github.io/sandwich/)为准。

## 版本前提

- Sandwich `2.4.0` 以 Kotlin `2.4.x` 编译，工程工具链为 Kotlin `2.4.20`；不升级 Kotlin 时最高只能安全使用 `2.1.3`，且该版本存在 Retrofit suspend 路径吞 `CancellationException` 的已知缺陷（`2.2.1` 修复）。
- 本地参考仓库：`../sandwich`（main 分支与 2.4.0 一致），对库行为的疑问直接读源码。

## 接入点（共三处）

```kotlin
// 1. gradle/libs.versions.toml + app/build.gradle.kts
implementation(platform(libs.sandwich.bom))   // 2.4.0
implementation(libs.sandwich)
implementation(libs.sandwich.retrofit)

// 2. NetworkFactory.kt —— Retrofit 挂上工厂，suspend 接口即可直接返回 ApiResponse<T>
.addCallAdapterFactory(ApiResponseCallAdapterFactory.create())

// 3. SmallApplication.onCreate —— 启动时注册一次、之后只读
SandwichInitializer.sandwichExceptionClassifiers += RetrofitExceptionClassifier
```

## ApiResponse 模型

| 分支 | 含义 | payload / 字段 |
|---|---|---|
| `Success<T>` | 传输成功 | `data`（反序列化 body）、`tag`（Retrofit 集成中是整个 `retrofit2.Response`） |
| `Failure.Error` | HTTP 层错误（非 2xx）或**信封业务失败** | `payload`：HTTP 错误为 `retrofit2.Response`；信封失败为 `WanAndroidError` |
| `Failure.Exception` | 客户端异常（断网/超时/解析失败） | `throwable` |

注意：`statusCode`/`errorBody` 扩展仅在 `payload is Response<*>` 时可用，对信封降级的 Error 调用会抛 `IllegalArgumentException`——判型后再取。

协程取消不会被吞：工厂、`mapSuccess`、链式扩展对 `CancellationException` 一律重抛。

## 信封模式（HTTP 200 + body 内业务码）

WanAndroid 约定 `errorCode != 0` 为业务失败。DTO 实现信封接口即可让 Sandwich 自动判定：

```kotlin
@Serializable
internal data class WanAndroidEnvelope<T>(
    val data: T? = null,
    val errorCode: Int = 0,
    val errorMsg: String = "",
) : ApiEnvelope<T?, WanAndroidError> {
    override val isEnvelopeSuccessful: Boolean get() = errorCode == 0
    override val envelopeBody: T? get() = data          // 可空，永不抛
    override val envelopeError: WanAndroidError get() = WanAndroidError(errorCode, errorMsg.ifBlank { null })
}
```

- `ApiEnvelopeMapper` 默认注册，业务失败自动降级为 `Failure.Error(payload = envelopeError)`。
- Repository 出口用 `unwrap()` 拍平为业务类型：

```kotlin
override suspend fun loadBanners(): ApiResponse<List<Banner>> =
    api.getBanners()
        .unwrap()                    // ApiResponse<List<WanBannerDto>?>
        .mapSuccess {
            orEmpty().distinctBy(WanBannerDto::id).map(WanBannerDto::toModel)
        }
        .onFailure { AppLogger.warning(TAG) { "loadBanners: ${failureSummary()}" } }
```

**Repository 永不抛异常**是硬契约：`envelopeBody` 保持可空、null 在 `mapSuccess` 里以 `orEmpty()` 兜底。已知例外见[已知边界](#已知边界与拍板)。

## 错误分类与脱敏

| 判断 | 落位 | 说明 |
|---|---|---|
| `isNetworkFailure` / `isTimeout` | Sandwich（依赖分类器注册） | 断网、超时分别对应 `SandwichNetworkException`、`SandwichTimeoutException` |
| `isConnectivityFailure()` | `core/data/network/ApiResponseExt.kt` | **断网 ∪ 超时**都按“网络不可用”反馈（产品拍板，保持与旧实现一致的用户可见行为） |
| `envelopeErrorCode()` | `data/content/repository/ContentRepository.kt` | 信封业务码提取，页面只拿 `Int`，不 import DTO |
| `failureSummary()` | 同上 | 日志白名单脱敏：业务错误只记错误码、HTTP 只记状态码、异常只记类名；`WanAndroidError.toString()` 只输出 code |

业务码常量表 `ApiCodes`（如 `TOKEN_EXPIRED = -1001`）位于 `core/data/network`，按后端文档维护；业务码到页面事件的翻译保留在 ViewModel。

## ViewModel 消费模式

- 单一 `ExploreUiState`（data class）经 `mutableUiState.update { copy() }` 原子更新，UI 一次收集。
- 分支用穷尽 `when (response)` 或 `onSuccess { }` 链，失败直接是值，不做异常装箱。
- 业务码翻译示例：`any { it.envelopeErrorCode() == ApiCodes.TOKEN_EXPIRED }` → `RequireLogin` 事件。

## 并发模式

| 工具 | 一句话 |
|---|---|
| `launch` | 干完就行，无结果 |
| `async` | 干完交货，可 `await` |
| `coroutineScope` | 等全部干完 |
| `awaitAll` | 等全部交货 |

核心原则：**失败是值，不是异常**。网络失败已被 Sandwich 值化，`coroutineScope` 里没有异常在飞，因此连坐机制不会触发，`supervisorScope` 没有存在必要（`viewModelScope` 本身就是 `SupervisorJob`，顶层并行天然互不连坐）。

并发取货的标准形态（类型各自保真，不用 `awaitAll` 以免熔成公共父类型）：

```kotlin
val (bannerRes, articleRes) = coroutineScope {
    val banners = async { repository.loadBanners() }
    val articles = async { repository.loadArticles(FIRST_ARTICLE_PAGE) }
    banners.await() to articles.await()
}
// 此后线性落地：成功照常、失败收集为纯函数、取消由 await 结构性传播
```

不需要结果的并发（如初始化时的轮播）用裸 `launch` 派发即可。

## 已知边界与拍板

- **HTTP 200 空响应体**：Sandwich 对"预期空体"的接口有官方正解——泛型直接声明 `ApiResponse<Unit>`，内置处理 204/空 body。本工程的风险仅存在于"预期有信封 body 的接口被服务端异常地回了空体"（契约违背场景）：Sandwich 以 `Unit as T` 造 `Success`，`unwrap()` 对其调信封方法会抛 `ClassCastException`，当前无兜底（2026-09-16 拍板接受）。若线上出现 `ClassCastException` 崩溃即为该路径；修复方案是在 repository 两方法外包 `try/catch` 归一为 `Failure.Exception`（约 8 行）。
- **协程取消**：`CancellationException` 是控制流不是错误，全链路重抛，调用方不需要也不应该捕获它。
- **错误响应体全量读取**：Sandwich 官方 `deserializeErrorBody`/`apiMessage` 是全量读；本工程不调用它们，日志走白名单摘要。

## 官方 API 速览（含未使用部分）

以下为本工程**未使用**的官方能力，记录触发条件，避免重复调研：

| API | 一句话 | 何时考虑引入 |
|---|---|---|
| `runAndRetry` + `RetryPolicies`（[Retry](https://skydoves.github.io/sandwich/retry/)） | 带策略的请求重试：固定间隔/线性/指数退避（含 jitter），`retryOn` 可只对超时等特定失败重试；`retryAfterMillis` 读服务端 `Retry-After` 头 | 幂等接口的弱网重试；注意与 OkHttp `retryOnConnectionFailure`（仅连接层）分工 |
| `then` / `suspendThen`（[Sequential](https://skydoves.github.io/sandwich/sequential/)） | 链式顺序请求：每步 lambda 拿到上一步结果 | 依赖链请求（token → 详情 → 列表），登录落地时大概率用上 |
| `merge()` + `ApiResponseMergePolicy`（[Merge](https://skydoves.github.io/sandwich/merge/)） | 合并多个 `ApiResponse<List<T>>`，`PREFERRED_FAILURE`（默认，任一失败即失败）或 `IGNORE_FAILURE` | 多页/多源一次合并；当前分页是增量追加，不适用 |
| 全局 Operator（[Operator](https://skydoves.github.io/sandwich/operator/) / [Global](https://skydoves.github.io/sandwich/global/)） | `ApiResponseSuspendOperator` 注册进 `SandwichInitializer.sandwichOperators`，对每个响应全局生效；官方示例即 401/403 → 刷新 token | 登录体系落地、token 失效需要全局动作时（业务码翻译目前有意保留在页面层） |
| 全局 Failure Mapper（[Mapper](https://skydoves.github.io/sandwich/mapper/)） | `SandwichInitializer.sandwichFailureMappers` 把错误统一映射为自定义 Failure 单例 | 第二个数据源或第二套错误词汇表出现时 |
| `sandwich-test`（[Testing](https://skydoves.github.io/sandwich/testing/)） | `ApiResponse.fakeSuccess/fakeError/fakeException` 造值 + `assertSuccess { }` 断言 DSL，无需 MockWebServer | 工程补测试时的标准工具；注意 fake 工厂会绕过全局 operator/mapper |
| `ApiResponse<Unit>`（[Empty Body](https://skydoves.github.io/sandwich/empty-body/)） | 空响应体接口的官方声明方式 | 新增删除/提交类无 body 接口时 |
| `getOrNull/getOrElse/getOrThrow`（[Retrieve](https://skydoves.github.io/sandwich/retrieve/)） | 从 ApiResponse 取值 | 已在用（`getOrNull`） |
| 版本迁移（[Migration](https://skydoves.github.io/sandwich/migration/)） | 大版本升级指南 | 升 2.x→3.x 时先读 |

## 官方文档

| 主题 | 链接 |
|---|---|
| 首页 / 下载 | <https://skydoves.github.io/sandwich/> |
| ApiResponse 模型 | <https://skydoves.github.io/sandwich/apiresponse/> |
| Retrofit 集成 | <https://skydoves.github.io/sandwich/retrofit/> |
| Ktor / Ktorfit 集成 | <https://skydoves.github.io/sandwich/ktor/> |
| BOM | <https://skydoves.github.io/sandwich/bom/> |
| 取值扩展 | <https://skydoves.github.io/sandwich/retrieve/> |
| Mapper | <https://skydoves.github.io/sandwich/mapper/> |
| Envelope（信封模式） | <https://skydoves.github.io/sandwich/envelope/> |
| Sequential（顺序请求） | <https://skydoves.github.io/sandwich/sequential/> |
| Retry（重试） | <https://skydoves.github.io/sandwich/retry/> |
| Operator（操作符） | <https://skydoves.github.io/sandwich/operator/> |
| Merge（合并） | <https://skydoves.github.io/sandwich/merge/> |
| Global Handling（全局处理） | <https://skydoves.github.io/sandwich/global/> |
| Testing（测试） | <https://skydoves.github.io/sandwich/testing/> |
| Empty Body（空响应体） | <https://skydoves.github.io/sandwich/empty-body/> |
| Migration Guide（迁移） | <https://skydoves.github.io/sandwich/migration/> |
| GitHub 仓库 | <https://github.com/skydoves/sandwich> |

## KMP 展望

迁移 KMP 时网络层只换传输集成：Retrofit → Ktorfit，`ApiResponseCallAdapterFactory` → `ApiResponseConverterFactory`；`ApiResponse`、信封、分类器、`unwrap` 等均在 commonMain，业务层与页面层代码原样保留。`sandwich-ktor` 覆盖 19 个 target（含 iOS/macOS/JS/Wasm，KMP 场景必须 `2.4.0+`，其修复了 Kotlin/Native 上全局配置静默失效的问题）。
