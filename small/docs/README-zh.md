![sandwich](https://user-images.githubusercontent.com/24237865/162602054-2010d249-8a81-4673-b9ae-1edff1080ab7.png)<br>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://github.com/skydoves/Sandwich/actions"><img alt="Build Status" src="https://github.com/skydoves/sandwich/actions/workflows/build.yml/badge.svg"/></a>
  <a href="https://github.com/doveletter"><img alt="Profile" src="https://skydoves.github.io/badges/dove-letter.svg"/></a><br>
  <a href="https://devlibrary.withgoogle.com/products/android/repos/skydoves-Sandwich"><img alt="Google" src="https://skydoves.github.io/badges/google-devlib.svg"/></a>
  <a href="https://skydoves.medium.com/handling-success-data-and-error-callback-responses-from-a-network-for-android-projects-using-b53a26214cef"><img alt="Medium" src="https://skydoves.github.io/badges/Story-Medium.svg"/></a>
  <a href="https://github.com/skydoves"><img alt="Profile" src="https://skydoves.github.io/badges/skydoves.svg"/></a>
  <a href="https://youtu.be/agjbbn9Swkc"><img alt="Profile" src="https://skydoves.github.io/badges/youtube-android-worldwide.svg"/></a> 
  <a href="https://skydoves.github.io/libraries/sandwich/html/sandwich/com.skydoves.sandwich/index.html"><img alt="Dokka" src="https://skydoves.github.io/badges/dokka-sandwich.svg"/></a>
</p>

## 为什么选择 Sandwich？
Sandwich 的诞生是为了简化标准化接口的创建过程，让你可以用统一的接口为 [Retrofit](https://skydoves.github.io/sandwich/sandwich/retrofit/)、[Ktor](https://skydoves.github.io/sandwich/sandwich/ktor/) 等各类网络库的响应建模。在多层架构中，这个库让你能够利用函数式操作符，更简洁地处理响应体数据、错误和异常情况。有了 Sandwich，你无需再创建 Resource 或 Result 之类的包装类，可以专注于核心业务逻辑。Sandwich 还具备[全局响应处理](https://skydoves.github.io/sandwich/operator#global-operator)、[Mapper](https://skydoves.github.io/sandwich/mapper)、[Operator](https://skydoves.github.io/sandwich/operator) 等特性，并且对协程（Coroutines）的兼容性十分出色，例如[配合协程的 ApiResponse](https://skydoves.github.io/sandwich/apiresponse/#apiresponse-extensions-with-coroutines)。

## 下载
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skydoves/sandwich.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.skydoves%22%20AND%20a:%22sandwich%22)

Sandwich 达成了一个了不起的里程碑：在世界各地的 Android 和后端项目中，下载量已__超过 1,200,000__！ <br>

<img src="https://user-images.githubusercontent.com/24237865/103460609-f18ee000-4d5a-11eb-81e2-17696e3a5804.png" width="774" height="224"/>

### Gradle

把下面的依赖添加到你**模块**的 `build.gradle` 文件中：

```gradle
dependencies {
    implementation(platform("com.github.skydoves:sandwich-bom:2.4.0"))

    implementation("com.github.skydoves:sandwich")
    implementation("com.github.skydoves:sandwich-retrofit") // 用于 Retrofit(Android)
    testImplementation("com.github.skydoves:sandwich-test") // 用于测试
}
```

对于 Kotlin Multiplatform，把下面的依赖添加到你模块的 `build.gradle.kts` 文件中：

```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation(project.dependencies.platform("com.github.skydoves:sandwich-bom:$version"))

            implementation("com.github.skydoves:sandwich")
            implementation("com.github.skydoves:sandwich-ktor")
            implementation("com.github.skydoves:sandwich-ktor-serialization")
            implementation("com.github.skydoves:sandwich-ktorfit")
        }
    }
    val commonTest by getting {
        dependencies {
            implementation("com.github.skydoves:sandwich-test")
        }
    }
}
```

## R8 / ProGuard
具体的规则[已经打包](sandwich/consumer-rules.pro)在 JAR 中，R8 可以自动解析。

## 文档

想全面了解 Sandwich 的细节，请查阅[这份完整文档](https://skydoves.github.io/sandwich/)。

## 用例
你也可以在下面的仓库中看到这个库的优秀用例：
- [Pokedex](https://github.com/skydoves/pokedex)：🗡️ 一个基于 MVVM 架构的 Android Pokedex 应用，使用了 Hilt、Motion、协程、Flow、Jetpack(Room、ViewModel、LiveData)。
- [ChatGPT Android](https://github.com/skydoves/chatgpt-android)：📲 ChatGPT Android 在 Android 上演示 OpenAI 的 ChatGPT，使用面向 Compose 的 Stream Chat SDK。
- [DisneyMotions](https://github.com/skydoves/DisneyMotions)：🦁 一款迪士尼应用，运用变换动画，基于 MVVM（ViewModel、协程、LiveData、Room、Repository、Koin）架构。
- [MarvelHeroes](https://github.com/skydoves/marvelheroes)：❤️ 一个漫威英雄示例应用，基于 MVVM（ViewModel、协程、LiveData、Room、Repository、Koin）架构。
- [Neko](https://github.com/CarlosEsco/Neko)：一款免费、开源、非官方的 Android MangaDex 阅读器。
- [TheMovies2](https://github.com/skydoves/TheMovies2)：🎬 一个使用 The Movie DB 的演示项目，基于 Kotlin MVVM 架构，并采用了 material design 与动画。

## 用法

想全面了解 Sandwich 的细节，请查阅[这份完整文档](https://skydoves.github.io/sandwich/)。

- [Retrofit 集成](https://skydoves.github.io/sandwich/retrofit)
- [Ktor 集成](https://skydoves.github.io/sandwich/ktor)
- [Ktorfit 集成](https://skydoves.github.io/sandwich/ktorfit)
- [测试](https://skydoves.github.io/sandwich/testing)

### ApiResponse

`ApiResponse` 是一个接口，旨在为 API 或 I/O 调用（比如网络、数据库等等）生成一致的响应。它提供了一系列便捷的扩展函数来管理你的 payload，覆盖响应体数据和异常两种场景。`ApiResponse` 包含三种不同的类型：**Success**、**Failure.Error** 和 **Failure.Exception**。

#### ApiResponse.Success

它表示 API 或 I/O 任务的成功响应。给定泛型类型和数据，就可以创建一个 [ApiResponse.Success] 实例。

```kotlin
val apiResponse = ApiResponse.Success(data = myData)
val data = apiResponse.data
```

根据你的模型设计，还可以使用 `tag` 属性。`tag` 是一个可以额外携带的值，用来区分数据来源，或者方便对成功数据进行后续处理。

```kotlin
val apiResponse = ApiResponse.Success(data = myData, tag = myTag)
val tag = apiResponse.tag
```

#### ApiResponse.Failure.Exception

它表示在客户端构建 API 请求或处理响应时，因意外异常而导致的失败任务，比如网络连接失败。你可以从 `ApiResponse.Failure.Exception` 中获取异常详情。

```kotlin
val apiResponse = ApiResponse.Failure.Exception(exception = HttpTimeoutException())
val exception = apiResponse.exception
val message = apiResponse.message
```

#### ApiResponse.Failure.Error

它表示一次失败的 API 或 I/O 请求，通常由无效请求或服务器内部错误导致。你还可以额外放入一个 error payload，里面可以承载详细的错误信息。

```kotlin
val apiResponse = ApiResponse.Failure.Error(payload = errorBody)
val payload = apiResponse.payload
```

你还可以定义继承 `ApiResponse.Failure.Error` 或 `ApiResponse.Failure.Exception` 的自定义错误响应，如下面的例子所示：

```kotlin
data object LimitedRequest : ApiResponse.Failure.Error(
  payload = "your request is limited",
)

data object WrongArgument : ApiResponse.Failure.Error(
  payload = "wrong argument",
)

data object HttpException : ApiResponse.Failure.Exception(
  throwable = RuntimeException("http exception")
)
```

当你想显式地定义并处理错误响应时，自定义错误响应非常有用，尤其是在配合 map 扩展函数使用时。

```kotlin
val apiResponse = service.fetchMovieList()
apiResponse.onSuccess {
    // ..
}.flatMap {
  // 如果 ApiResponse 是 Failure.Error 并且包含错误响应体，就把它映射为自定义错误响应。
  if (this is ApiResponse.Failure.Error) {
    val errorBody = (payload as? Response)?.body?.string()
    if (errorBody != null) {
      val errorMessage: ErrorMessage = Json.decodeFromString(errorBody)
      when (errorMessage.code) {
        10000 -> LimitedRequest
        10001 -> WrongArgument
      }
    }
  }
  this
}
```

然后你就可以在其他层里根据自定义消息来处理这些错误：

```kotlin
val apiResponse = repository.fetchMovieList()
apiResponse.onError {
  when (this) {
    LimitedRequest -> // 更新你的 UI
    WrongArgument -> // 更新你的 UI
  }
}
```

你可能不想对每个 API 请求都使用 `flatMap` 扩展函数。如果你想让自定义错误类型在所有 API 请求中保持统一，可以了解一下[全局 Failure 映射器](https://skydoves.github.io/sandwich/mapper/#global-failure-mapper)。

#### 创建 ApiResponse

Sandwich 提供了便捷的方式来创建 `ApiResponse`，比如 `ApiResponse.of` 或 `apiResponseOf` 这样的函数，如下所示：

```kotlin
val apiResponse = ApiResponse.of { service.request() }
val apiResponse = apiResponseOf { service.request() }
```

如果你需要在 lambda 里运行挂起函数，可以改用 `ApiResponse.suspendOf` 或 `suspendApiResponseOf`：

```kotlin
val apiResponse = ApiResponse.suspendOf { service.request() }
val apiResponse = suspendApiResponseOf { service.request() }
```

> **注意**：如果你打算在 Sandwich 中使用全局操作符或全局 ApiResponse 映射器，就应该用 `ApiResponse.of` 或 `ApiResponse.suspendOf` 方法来创建 `ApiResponse`，以确保这些全局函数能够生效。如果你使用的是 `ApiResponseFailureSuspendMapper` 或 `ApiResponseSuspendOperator`（在 Ktor/Ktorfit 中很常见），请使用 `ApiResponse.suspendOf`，以确保挂起映射器和操作符会被正确地等待。

#### ApiResponse 扩展函数

你可以用下面这些扩展函数有效地处理 `ApiResponse`：

- **onSuccess**：当 `ApiResponse` 为 `ApiResponse.Success` 类型时执行。在这个作用域里，你可以直接访问响应体数据。
- **onError**：当 `ApiResponse` 为 `ApiResponse.Failure.Error` 类型时执行。在这里你可以访问 `messageOrNull` 和 `payload`。
- **onException**：当 `ApiResponse` 为 `ApiResponse.Failure.Exception` 类型时执行。在这里你可以访问 `messageOrNull` 和 `exception`。
- **onFailure**：当 `ApiResponse` 为 `ApiResponse.Failure.Error` 或 `ApiResponse.Failure.Exception` 时执行。在这里你可以访问 `messageOrNull`。

每个作用域都根据对应的 `ApiResponse` 类型运行：

```kotlin
val response = disneyService.fetchDisneyPosterList()
response.onSuccess {
    // 请求成功时会执行这个作用域。
    // 处理成功的情况
  }.onError {
    // 请求因错误而失败时会执行这个作用域。
    // 处理错误的情况
  }.onException {
   // 请求因异常而失败时会执行这个作用域。
   // 处理异常的情况
  }
```

如果不想逐一区分失败情况，可以用 `onFailure` 扩展函数来简化：

```kotlin
val response = disneyService.fetchDisneyPosterList()
response.onSuccess {
    // 请求成功时会执行这个作用域。
    // 处理成功的情况
  }.onFailure {
      
  }
```

#### 配合协程的 ApiResponse 扩展函数

借助 `ApiResponse` 类型，你可以利用 [协程（Coroutines）](https://kotlinlang.org/docs/coroutines-overview.html) 扩展函数，在协程作用域中无缝处理响应。这些扩展函数为处理不同的响应类型提供了一种便捷的方式。用法如下：

- **suspendOnSuccess**：当 `ApiResponse` 为 `ApiResponse.Success` 类型时，这个扩展函数会执行。你可以在这个作用域内直接访问响应体数据。

- **suspendOnError**：当 `ApiResponse` 为 `ApiResponse.Failure.Error` 类型时，会执行这个扩展函数。你可以在这个作用域内访问错误消息和错误响应体。

- **suspendOnException**：当 `ApiResponse` 为 `ApiResponse.Failure.Exception` 类型时，会触发这个扩展函数。你可以在这个作用域内访问异常消息。

- **suspendOnFailure**：当 `ApiResponse` 为 `ApiResponse.Failure.Error` 或 `ApiResponse.Failure.Exception` 时，会执行这个扩展函数。你可以在这个作用域内访问错误消息。

每个扩展函数的作用域都基于对应的 `ApiResponse` 类型运行。通过这些扩展函数，你可以在不同的协程上下文中有效地处理响应。

```kotlin
flow {
  val response = disneyService.fetchDisneyPosterList()
  response.suspendOnSuccess {
    posterDao.insertPosterList(data) // insertPosterList(data) 是一个挂起函数。
    emit(data)
  }.suspendOnError {
    // 处理错误情况
  }.suspendOnException {
    // 处理异常情况
  }
}.flowOn(Dispatchers.IO)
```

#### Flow

Sandwich 提供了一些实用的扩展函数，可以用 `toFlow` 扩展函数把你的 `ApiResponse` 转换成 [Flow](https://kotlinlang.org/docs/flow.html)：

```kotlin
val flow = disneyService.fetchDisneyPosterList()
  .onError {
    // API 请求收到错误响应时处理错误情况。
  }.onException {
    // API 请求收到异常响应时处理异常情况。
  }.toFlow() // 返回一个协程 Flow
  .flowOn(Dispatchers.IO)
```

如果你想转换原始数据，并使用包含转换后数据的 `Flow`，可以像下面的示例这样做：

```kotlin
val response = pokedexClient.fetchPokemonList(page = page)
response.toFlow { pokemons ->
  pokemons.forEach { pokemon -> pokemon.page = page }
  pokemonDao.insertPokemonList(pokemons)
  pokemonDao.getAllPokemonList(page)
}.flowOn(Dispatchers.IO)
```

#### 函数式扩展

Sandwich 提供了多种函数式扩展，用于转换和组合 `ApiResponse`：

- **恢复（Recovery）**：`recover`、`recoverWith`，用兜底数据把失败恢复为成功
- **校验（Validation）**：`validate`、`requireNotNull`，校验成功响应中的数据，数据无效则转为失败
- **过滤（Filter）**：`filter`、`filterNot`，过滤成功响应中列表数据里的元素
- **组合（Zip/Combine）**：`zip`、`zip3`，把多个 `ApiResponse` 实例合并为一个
- **观察（Peek/Tap）**：`peek`、`peekSuccess`、`peekFailure`、`peekError`、`peekException`，只观察响应而不修改它

```kotlin
val response = disneyService.fetchDisneyPosterList()
  .validate({ it.isNotEmpty() }) { "List cannot be empty" }  // 校验数据
  .filter { poster -> poster.isActive }                       // 过滤列表元素
  .recover(emptyList())                                       // 用兜底数据恢复
  .peekSuccess { posters -> analytics.track(posters.size) }   // 副作用
```

所有扩展函数都有对应的 `suspend` 变体（例如 `suspendRecover`、`suspendValidate`）来支持协程。完整细节请参阅 [ApiResponse 文档](https://skydoves.github.io/sandwich/apiresponse/)。

### 数据获取

Sandwich 提供了轻松的方法，可以直接从 `ApiResponse` 中提取封装的响应体数据。你可以利用以下功能：

#### getOrNull
如果实例是 `ApiResponse.Success`，返回封装的数据；如果失败，返回 null。

```kotlin
val data: List<Poster>? = disneyService.fetchDisneyPosterList().getOrNull()
```

#### getOrElse
如果实例是 `ApiResponse.Success`，返回封装的数据；如果失败，返回默认值。

```kotlin
val data: List<Poster> = disneyService.fetchDisneyPosterList().getOrElse(emptyList())
```

#### getOrThrow
如果实例是 `ApiResponse.Success`，返回封装的数据；如果失败，抛出封装的 `Throwable` 异常。

```kotlin
try {
  val data: List<Poster> = disneyService.fetchDisneyPosterList().getOrThrow()
} catch (e: Exception) {
  e.printStackTrace()
}
```

### 重试

Sandwich 提供了无缝的方式来运行和重试任务。要执行并重试网络或 I/O 请求，你可以使用 `RetryPolicy` 接口和 `runAndRetry` 扩展函数，如下面的代码所示：

```kotlin
val retryPolicy = object : RetryPolicy {
  override fun shouldRetry(attempt: Int, message: String?): Boolean = attempt <= 3

  override fun retryTimeout(attempt: Int, message: String?): Int = 3000
}

val apiResponse = runAndRetry(retryPolicy) { attempt, reason ->
  mainRepository.fetchPosters()
}.onSuccess {
  // 处理成功的情况
}.onFailure {
  // 处理失败的情况
}
```

通过这套配置，你可以定义重试策略，决定是否进行重试并指定重试的超时时间。`runAndRetry` 扩展函数会封装执行逻辑，应用所定义的策略，并以整洁、结构化的方式提供响应。

### 顺序执行

当你需要按顺序执行网络请求时，Sandwich 提供了顺序执行的解决方案。

#### then 与 suspendThen

如果你遇到需要按依赖关系顺序执行任务 A、B、C 的场景，比如任务 B 依赖任务 A 完成，任务 C 依赖任务 B 完成，就可以使用 `then` 或 `suspendThen` 扩展函数，如下面的示例所示：

```kotlin
service.getUserToken(id) suspendThen { tokenResponse ->
    service.getUserDetails(tokenResponse.token) 
} suspendThen { userResponse ->
    service.queryPosters(userResponse.user.name)
}.mapSuccess { posterResponse ->
  posterResponse.posters
}.onSuccess {
    posterStateFlow.value = data
}.onFailure {
    Log.e("sequential", message())
}
```

### 操作符（Operator）

**操作符（Operator）** 是 Sandwich 提供的最强大的能力之一。你可以用它为 `ApiResponse` 实例建立定义清晰、预先配置好的处理器，从而把一套统一的处理流程封装起来，在各个 API 请求中复用。

通过 `operator` 扩展函数配合 `ApiResponseOperator`，你可以简化 `onSuccess`、`onError` 和 `onException` 场景的处理。当你想对 `ApiResponse` 实例做全局处理，并希望减少 `ViewModel` 和 `Repository` 类里的样板代码时，**操作符**尤其有用。下面是几个示例：

```kotlin
/** 一个通用响应操作符，无论哪种类型的 [ApiResponse] 都能处理。 */
class CommonResponseOperator<T>(
  private val success: suspend (ApiResponse.Success<T>) -> Unit
) : ApiResponseOperator<T>() {

  // 在 API 请求收到错误响应时处理错误场景。
  override fun onSuccess(apiResponse: ApiResponse.Success<T>) = success(apiResponse)

  // 根据状态码处理错误场景。
  // 例如服务器内部错误。
  override fun onError(apiResponse: ApiResponse.Failure.Error) {
    apiResponse.run {
      Timber.d(message())
      
      // 使用映射器将 ApiResponse.Failure.Error 映射为自定义的错误模型。
      map(ErrorEnvelopeMapper) {
        Timber.d("[Code: $code]: $message")
      }
    }
  }

  // 在 API 请求收到异常响应时处理异常场景。
  // 例如网络连接错误、超时。
  override fun onException(apiResponse: ApiResponse.Failure.Exception) {
    apiResponse.run {
      Timber.d(message())
    }
  }
}

disneyService.fetchDisneyPosterList().operator(
    CommonResponseOperator(
      success = {
        emit(data)
        Timber.d("success data: $data")
     }
    )
)
```

采用**操作符**模式后，你可以大幅简化对各种 `ApiResponse` 结果的管理，让应用的架构更整洁、更易维护。

#### 配合协程的操作符

如果你想用操作符模式来委托并执行一个挂起 lambda，就要用到 `suspendOperator` 扩展函数和 `ApiResponseSuspendOperator` 类。它们能让这件事变得简单，示例如下：

```kotlin
class CommonResponseOperator<T>(
  private val success: suspend (ApiResponse.Success<T>) -> Unit
) : ApiResponseSuspendOperator<T>() {

  // 在 API 请求收到成功响应时处理成功场景。
  override suspend fun onSuccess(apiResponse: ApiResponse.Success<T>) = success(apiResponse)

  // ... //
}
```

你可以在 `success` 作用域里使用 `emit` 这样的挂起函数。

```kotlin
val response = disneyService.fetchDisneyPosterList().suspendOperator(
    CommonResponseOperator(
      success = {
        emit(data)
        Timber.d("success data: $data")
      }
    )
)
```

把 **suspendOperator** 扩展函数和 **ApiResponseSuspendOperator** 类结合起来，你就能在操作符模式下高效管理挂起 lambda，让代码库更加简洁、易于维护。

#### 全局操作符

全局操作符无疑是 Sandwich 提供的一项强大特性。借助 `SandwichInitializer`，你可以对应用中所有 `ApiResponse` 实例全局执行操作符。这样一来，就不必为每次 API 调用都创建操作符实例，也不用为了通用操作引入依赖注入。下面的示例展示了如何用全局操作符同时处理 `ApiResponse.Failure.Error` 和 `ApiResponse.Failure.Exception` 两种情况。你可以利用全局操作符刷新用户 token，或者在应用中为特定 API 请求实现其他任何需要的附加流程。下面的例子演示了如何使用 Sandwich 的全局操作符，根据响应状态自动检查并刷新用户 token：

##### 初始化全局操作符

首先，强烈建议在 Application 类中初始化全局操作符，或者使用 [App Startup](https://developer.android.com/topic/libraries/app-startup) 这类初始化方案。这样可以确保全局操作符在任何 API 请求发出之前完成设置。

```kotlin
class SandwichDemoApp : Application() {

  override fun onCreate() {
    super.onCreate()
    
    // 我们只会处理错误和异常这两种情况，
    // 所以不必在意操作符的泛型类型。
    SandwichInitializer.sandwichOperators += listOf(TokenRefreshGlobalOperator<Any>(this))

    // ... //
  }
}
```

通过在 `SandwichInitializer` 中配置全局操作符，你的应用就能统一处理各种 `ApiResponse` 情况，比如在全局层面管理成功场景、处理错误或异常。

##### 实现你的全局操作符

创建你自己的 `GlobalResponseOperator` 类，继承 `ApiResponseSuspendOperator` 或 `ApiResponseOperator` 这类操作符。这个操作符让你可以定义能够全局应用的通用响应处理逻辑。

```kotlin
class TokenRefreshGlobalOperator<T> @Inject constructor(
  private val context: Context,
  private val authService: AuthService,
  private val userDataStore: UserDataStore,
  coroutineScope: CoroutineScope,
) : ApiResponseSuspendOperator<T>() {

  private var userToken: UserToken? = null

  init {
    coroutineScope.launch {
      userDataStore.tokenFlow.collect { token ->
        userToken = token
      }
    }
  }

  override suspend fun onError(apiResponse: ApiResponse.Failure.Error) {
    // 检查当前请求之前是否是带认证信息的请求
    apiResponse.headers["Authorization"] ?: return

    // 如果错误响应为 Unauthorized 或 Forbidden，则刷新访问 token
    when (apiResponse.statusCode) {
      StatusCode.Unauthorized, StatusCode.Forbidden -> {
        userToken?.let { token ->
          val result = authService.refreshToken(token)
          result.onSuccessSuspend { data ->
            userDataStore.updateToken(
              UserToken(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
              ),
            )
            toast(R.string.toast_refresh_token_succeed)
          }.onFailureSuspend {
            toast(R.string.toast_refresh_token_failed)
          }
        }
      }
      else -> Unit
    }
  }

  override suspend fun onSuccess(apiResponse: ApiResponse.Success<T>) = Unit

  override suspend fun onException(apiResponse: ApiResponse.Failure.Exception) = Unit

  private suspend fun toast(@StringRes resource: Int) = withContext(Dispatchers.Main) {
    Toast.makeText(context, resource, Toast.LENGTH_SHORT).show()
  }
}
```

在这个例子中，全局操作符的 `onError` 函数会自动检查错误响应中是否出现 `Unauthorized` 和 `Forbidden` 状态码（HTTP 401 和 403）。一旦发生未授权错误，就会刷新用户 token，并通过 runAndRetry 用更新后的 token 重试失败的请求。这样，你就可以无缝地管理 API 请求的 token 过期与刷新。

#### 用 Hilt 和 App Startup 配置全局操作符

如果你想用 [Hilt](https://dagger.dev/hilt/) 和 [App Startup](https://developer.android.com/topic/libraries/app-startup) 来初始化全局操作符，可以按照下面的说明操作。

##### 1. 实现 Entry Point

首先，你需要实现一个 Entry Point，用于把全局操作符注入到 App Startup initializer 中。

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkEntryPoint {

  fun inject(networkInitializer: NetworkInitializer)

  companion object {

    fun resolve(context: Context): NetworkEntryPoint {
      val appContext = context.applicationContext ?: throw IllegalStateException(
        "applicationContext was not found in NetworkEntryPoint",
      )
      return EntryPointAccessors.fromApplication(
        appContext,
        NetworkEntryPoint::class.java,
      )
    }
  }
}
```

##### 2. 提供全局操作符依赖

接着，像下面的示例那样用 Hilt 提供你的全局操作符：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideTokenRefreshGlobalOperator(
    @ApplicationContext context: Context,
    authService: AuthService,
    userDataStore: UserDataStore
  ): TokenRefreshGlobalOperator<Any> {
    return TokenRefreshGlobalOperator(
      context = context,
      authService = authService,
      userDataStore = userDataStore,
      coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )
  }
}
```

##### 3. 实现 App Startup Initializer

最后，实现 App Startup Initializer，并按照 [App Startup 指南](https://developer.android.com/topic/libraries/app-startup#manual)完成初始化。

```kotlin
public class NetworkInitializer : Initializer<Unit> {

  @set:Inject
  internal lateinit var tokenRefreshGlobalOperator: TokenRefreshGlobalOperator<Any>

  override fun create(context: Context) {
    NetworkEntryPoint.resolve(context).inject(this)

    SandwichInitializer.sandwichOperators += listOf(tokenRefreshGlobalOperator)
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
```

## 觉得这个库好用？:heart:
欢迎加入本仓库的 __[stargazers](https://github.com/skydoves/sandwich/stargazers)__ 行列来支持它。:star: <br>
也欢迎 __[关注我](https://github.com/skydoves)__，期待我的下一个作品！🤩

# License
```xml
Copyright 2020 skydoves (Jaewoong Eum)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
