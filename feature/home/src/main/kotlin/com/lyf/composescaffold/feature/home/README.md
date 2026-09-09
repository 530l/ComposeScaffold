# Compose 状态提升与函数参数速查

写 Compose 组件时，优先分清三件事：**状态决定显示什么，回调通知发生了什么，插槽决定由外部提供哪一段 UI。**

本文用于阅读和编写组件，不改变首页运行逻辑。示例彼此独立，部分省略 import；教学示例直接使用中文字符串，实际业务文案应放入 `strings.xml`，通过 `stringResource` 获取。

配套的可编译案例位于同目录 [`samples/`](samples/) 包：每个文件对应本文一节，补全了 import 并附带 `@Preview`，可在 Android Studio 中直接预览；案例仅用于阅读，不接入运行中的首页。

## 1. 常用参数怎么选

| 参数写法 | 含义 | 常见用途 |
| --- | --- | --- |
| `expanded: Boolean` | 当前状态值 | 歌词是否展开 |
| `onClick: () -> Unit` | 无参数事件通知 | 点击、关闭、重试 |
| `onValueChange: (String) -> Unit` | 传出希望修改的新值 | 输入框 |
| `onExpandedChange: (Boolean) -> Unit` | 请求修改开关状态 | 展开、选中、启用 |
| `onSeek: (Long) -> Unit` | 传出目标位置 | 拖动播放进度 |
| `onSelected: (Item) -> Unit` | 传出业务对象 | 点击列表项 |
| `positionProvider: () -> Long` | 调用时获取值 | 按需读取播放位置 |
| `itemKey: (Item) -> String` | 输入对象并返回结果 | 提取稳定标识 |
| `content: @Composable () -> Unit` | 外部提供 UI | 卡片内容插槽 |
| `content: @Composable ColumnScope.() -> Unit` | 带布局接收者的 UI 插槽 | 内容可以使用 `ColumnScope` 能力 |

`Item` 是业务类型的占位名称，使用时替换成真实类型。

函数类型的通用写法是 `(参数类型) -> 返回类型`：

```kotlin
() -> Unit             // 不接收参数，不提供有意义的返回结果。
(Boolean) -> Unit      // 接收一个 Boolean，通常用于通知状态变化。
() -> Boolean         // 不接收参数，调用后返回一个 Boolean。
(String, Int) -> Unit  // 接收两个参数，例如作品标识和列表索引。
```

接收函数参数或返回函数的函数称为高阶函数。`onLyrics: () -> Unit` 只是一个函数类型参数；`on` 前缀是事件命名惯例，本身不会自动绑定点击。

```kotlin
onLyrics()                         // 立即调用。
TextButton(onClick = onLyrics) { }  // 传给按钮，点击时才调用。
```

## 2. 状态提升：状态值 + 修改回调

这是最常用的组件契约：`value: T` 与 `onValueChange: (T) -> Unit`。

```kotlin
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
private fun LyricsPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    TextButton(onClick = { onExpandedChange(!expanded) }) {
        Text(if (expanded) "收起歌词" else "展开歌词")
    }

    if (expanded) {
        Text("这里显示歌词内容")
    }
}

@Composable
private fun MusicExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LyricsPanel(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    )
}
```

数据流是：

```text
父级状态 expanded → 子组件显示
子组件点击 → onExpandedChange(新值) → 父级决定是否更新 → 子组件接收新状态
```

子组件请求修改，父级才是真正的状态持有者。父级可以接受、校验或拒绝请求，调用回调不等于状态已经改变。

不要再在子组件内部用 `remember { mutableStateOf(expanded) }` 复制一份相同状态：这样会出现两份状态，外部后续变化也不会自动同步到这份初始化副本。需要独立的编辑草稿时，应明确草稿的提交、取消与同步规则。

### 状态放在哪里

- 只影响单个组件的临时状态，可以留在组件内部。
- 多个组件共同读写的状态，提升到它们共同的上层。
- 简单、可保存的 UI 状态可以用 `rememberSaveable`；普通 `remember` 仅保留当前组合中的值，不保证 Activity 重建后恢复。
- 涉及业务加载、持久化或业务规则时，通常由 ViewModel 持有状态，通过 Intent 处理操作。

当前 [HomeScreen.kt](presentation/HomeScreen.kt) 是静态占位页，不需要为了套用模式而增加 ViewModel。

### 输入框是同一种模式

```kotlin
@Composable
private fun TitleEditor(
    title: String,
    onTitleChange: (String) -> Unit,
) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
    )
}
```

`onTitleChange` 可以直接转交，也可以包装后调用：`onValueChange = { newTitle -> onTitleChange(newTitle) }`。没有额外逻辑时直接转交即可。

## 3. 通过函数获取状态：`() -> T`

外部传状态值是“给你当前值”，传 supplier 是“给你一个读取入口”。

```kotlin
@Composable
private fun LyricsPanelWithProvider(
    expandedProvider: () -> Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val expanded = expandedProvider()

    TextButton(onClick = { onExpandedChange(!expanded) }) {
        Text(if (expanded) "收起歌词" else "展开歌词")
    }
}

@Composable
private fun ProviderExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LyricsPanelWithProvider(
        expandedProvider = { expanded },
        onExpandedChange = { expanded = it },
    )
}
```

**普通展示参数优先传 `expanded: Boolean`。** 需要由调用方决定读取时机时，再使用 `() -> T`，例如在点击发生时读取播放器的实时位置。

注意下面几个边界：

- `() -> T` 本身不是可观察状态，不会自动监听变化。
- 示例能够响应变化，是因为在组合期间调用 provider 时读取了 Compose `State`。
- 如果 provider 只返回普通可变变量，该变量变化不会自动通知 Compose。
- 在组件函数体里立即调用 provider，仍然是在组合阶段读取状态；仅仅换成 lambda 不保证减少重组。
- provider 返回的是其实现实际读取的值。若闭包捕获了旧的普通值，调用时仍可能返回旧值；不能把 lambda 自动等同于“永远最新”。

### 长期运行的副作用需要最新回调

已有任务不应因回调变化而重新计时时，可以用 `rememberUpdatedState`：

```kotlin
@Composable
private fun DelayedNotice(
    delayMs: Long,
    onElapsed: () -> Unit,
) {
    val currentOnElapsed by rememberUpdatedState(onElapsed)

    LaunchedEffect(delayMs) {
        delay(delayMs)
        currentOnElapsed()
    }
}
```

这个例子需要 `androidx.compose.runtime.LaunchedEffect`、`rememberUpdatedState` 和 `kotlinx.coroutines.delay`。延迟变化会重启任务；仅回调变化不重启任务，任务完成时读取最新回调。应按任务语义选择 effect 的 key，不要机械使用 `LaunchedEffect(Unit)`。

## 4. 事件回调：通知操作，不一定传出新状态

下面两种契约都常用，选取决于组件想表达什么：

```kotlin
onExpandedChange: (Boolean) -> Unit  // 请求设置为指定状态。
onToggle: () -> Unit                // 通知发生了切换操作，由外部根据最新状态计算。
```

本项目的业务按钮通常通过 Intent 接入 ViewModel：

```kotlin
// 组件只关心点击，不知道存储或业务实现。
onLike = { viewModel.onIntent(FeedIntent.ToggleLike(item.key)) }
```

这是 browse 调用方的说明片段，不能直接作为 home 的实现：Feature 之间不能直接依赖。可参考 `FeedInteractionButtons` 的设计，它接收 `interaction`、`busy` 与 `onLike/onSave/onRead`，成功状态和保存中的状态由外部控制。

组件的 `onClick` 一般保持 `() -> Unit`。需要异步业务操作时，在 ViewModel 内使用 `viewModelScope`；不要在组合函数体中直接发起业务请求。`suspend () -> Unit` 属于挂起函数类型，不能直接当作普通按钮的 `onClick` 使用。

## 5. UI 插槽：外部决定内容长什么样

```kotlin
@Composable
private fun MusicCard(
    title: String,
    actions: @Composable () -> Unit,
) {
    Column {
        Text(title)
        actions()
    }
}

@Composable
private fun SlotExample(onLyrics: () -> Unit) {
    MusicCard(title = "歌曲名称") {
        TextButton(onClick = onLyrics) {
            Text("查看歌词")
        }
    }
}
```

最后一个参数是函数时，可以使用尾随 lambda，把 `{ ... }` 放到括号外。这里相当于 `actions = { ... }`。

普通 `() -> Unit` 用于事件回调；`@Composable () -> Unit` 可以提供 Compose UI，必须从组合上下文调用，不能当作点击回调执行。

当前首页也有真实例子：`Scaffold(topBar = { ... })` 把顶部栏交给外部定义，`CenterAlignedTopAppBar(title = { ... })` 把标题 UI 交给外部定义。

### 带接收者的插槽

```kotlin
@Composable
private fun VerticalCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(content = content)
}
```

`ColumnScope.()` 表示调用插槽时提供 `ColumnScope` 接收者，因此插槽里可以使用该布局作用域的能力，例如 `Modifier.weight(...)`。它描述布局能力，与是否提升业务状态是两回事。

### 嵌套作用域要先读取外层约束

`BoxWithConstraints` 内再放 `Column` 时，两个布局作用域可能受到 DSL marker 的隐式接收者限制。需要外层约束时，先计算普通值，再传入内层：

```kotlin
BoxWithConstraints {
    val showLyrics = maxHeight >= 160.dp

    Column {
        if (showLyrics) {
            Text("歌词预览")
        }
    }
}
```

这样既明确了 `maxHeight` 来自外层，也避免在内层直接访问时出现 `cannot be called in this context with an implicit receiver`。

## 6. 写组件前的快速判断

1. 组件要显示什么？用 `value: T` 传入。
2. 用户会执行什么操作？用 `onXxx: () -> Unit` 或 `onXxxChange: (T) -> Unit` 传出。
3. 是否需要控制读取时机？有明确需要才用 `provider: () -> T`。
4. 外部是否需要定制一段 UI？用 `@Composable () -> Unit` 插槽。
5. 状态是否有多个读写方？确定共同的状态持有者，避免重复保存。

默认从 **状态值 + 事件回调** 开始，按实际需求增加 provider、插槽或 ViewModel。
