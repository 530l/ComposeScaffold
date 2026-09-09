# Browse：音乐、MV 与混合流

本模块负责列表、页面、音乐队列和 Feed 播放调度。底层播放、缓存、Surface 和系统会话由 [core:player](../../core/player/README.md) 实现。

目前音乐模式是可分页的音乐大厅；MV 和混合模式使用 VerticalPager。三种模式共用数据与互动逻辑，不复制三套播放器。入口保持 BrowseRoute.Main。

## 整体调用关系

```mermaid
flowchart TD
    Screen[BrowseScreen] --> List[BrowseViewModel]
    List --> Repo[FeedRepository]
    List --> Paging[每模式一个 LoadableController]
    Screen --> Hall[音乐大厅]
    Screen --> Pager[MV / 混合 Pager]
    Hall --> Intent[PlayMusicTrack Intent]
    Intent --> Controller[MusicPlayerController]
    Pager --> Playback[FeedPlaybackViewModel]
    Playback --> Session[FeedPlaybackCoordinator]
    Session -->|音乐项| Controller
    Controller --> Music[GlobalMusicPlayer]
    Session -->|MV 项| Mv[MvPlaybackController]
    Mv --> Pool[页面播放器池]
    Music --> MusicPool[音乐单播放器池]
    Pool --> Core[core:player Player 接口]
    MusicPool --> Core
    Core --> Exo[Media3 实现 / 共享缓存 / 系统会话]
```

app 的 PlayerModule 将 Media3PlayerFactory 作为单例 PlayerFactory 注入，同时提供公共 HTTP 客户端、IO 调度器和 MediaSessionOwner。Browse 不直接引用 androidx.media3 类型。

MusicPlayerController 接口位于 core:data，实现 GlobalMusicPlayer 位于本 Feature；app 的迷你播放器通过该接口控制音乐。MusicPlayerModule 使用进程级 Main.immediate 协程作用域，页面退出不会取消全局音乐。

## 音乐怎样使用 core:player

1. 音乐大厅把加载到的 FeedItem 映射为 MusicTrack，点击后发送 PlayMusicTrack Intent。
2. BrowseViewModel 通过 FeedMusicQueue 处理点击：新曲目提交 playTrack(track, queue)，当前曲目暂停/续播，失败曲目调用 resume 重试。
3. GlobalMusicPlayer 更新队列和当前索引，取消上一首的任务并归还播放器。
4. 通过 PlayerFactory.createPool(1) 创建或复用单播放器池。
5. 将 MusicTrack 映射为 MediaSource：id、url、标题、作者、封面及 AUDIO 类型。
6. acquire(..., PlaybackOptions(backgroundPlayback = true)) 预备媒体，再按当前 wantsPlay 调用 setPlaying。
7. 收集 Player.state，并在播放时每 250ms 读取 position，生成 MusicPlaybackSnapshot。

对应的核心调用：

```kotlin
val pool = factory.createPool(1)
val player = pool.acquire(
    source = mediaSource,
    options = PlaybackOptions(backgroundPlayback = true),
)
player.setPlaying(true)
```

这是调用方式示例；实际实现会检查请求代次、取消任务并管理归还，不能在 Composable 每次重组时创建池。

队列的上一首、下一首、单曲循环和列表模式由 GlobalMusicPlayer / MusicQueue 处理。音乐播放器本身不启用 repeatOne：收到 ended 后，由队列决定 Seek 回开头或播放下一首。clearQueue 会取消任务、归还播放器并释放池。

音乐大厅只收集曲目标识、播放意图、播放/缓冲/错误阶段，不订阅每 250ms 更新的完整进度。迷你播放器和混合 Feed 中的音乐项使用同一个全局音乐播放器，不另建音频实例。

## MV 怎样使用 core:player

1. FeedPagerRoute 从 FeedPlaybackViewModel 获取页面会话，绑定可见性和生命周期。
2. ObserveFeedPager 在滚动停止后用 settledPage 选播；targetPage 等信息交给 FeedPlaybackPolicy 决定预备项。
3. FeedPlaybackCoordinator 选中 MV 项时交给 MvPlaybackController，由 FeedMediaSource 将 FeedItem 映射为通用 MediaSource，MV 使用 VIDEO 类型和视频地址。
4. 页面池通常容量为 2：当前项与一个预备项。低内存设备或解码失败后降为 1。
5. acquire(source, position, PlaybackOptions(repeatOne = true)) 预备单条循环视频，不开启后台播放。
6. 当前项满足页面可见、主要可见和 wantsPlay 时，先暂停全局音乐，再调用 Player.setPlaying(true)。
7. Player.video 放入 outputs[key]，页面通过 VideoOutput.Render 显示画面。

```kotlin
val pool = factory.createPool(capacity = 2)
val player = pool.acquire(
    source = mediaSource,
    positionMs = savedPosition,
    options = PlaybackOptions(repeatOne = true),
)
```

页面通过 PlaybackCommands 执行 toggle、seekTo、retry，不接触池和 ExoPlayer。只有会话申请或归还播放器；Pager 预组合的相邻页面只读取输出接口。

FeedVideoPlayerSurface 根据方向字段预留比例，再使用 VideoOutput.aspectRatio 校正，完整显示视频。封面由 covered 控制，不能用 READY 或 outputVersion 代替首帧判断。背景加载小尺寸封面，不逐帧模糊视频。

MV 离屏立即暂停，后台停留 10 秒后释放池；退出 Composition 时 close 立即归还。重新进入时可重新绑定会话，通过书签恢复。进度、缓冲提示分别更新：进度间隔 250ms，缓冲提示延迟 300ms。

## 混合流与状态归属

混合流使用同一个 Pager。选中音乐项时，会话释放 MV 池，将音乐交给 GlobalMusicPlayer，再把全局音乐状态转换为 Feed 状态；选中 MV 时取消音乐观察，重新使用页面池。

| 状态 / 资源 | 管理者 | 生命周期 |
| --- | --- | --- |
| 模式、分页、列表、互动 | BrowseViewModel | 导航 Entry |
| 播放状态入口、各模式书签 | FeedPlaybackViewModel / SavedStateHandle | 导航 Entry；书签可恢复 |
| Pager / 音乐列表滚动位置 | Compose 状态 | 页面及其状态保存范围 |
| MV 借用记录、预备方向、播放意图 | FeedPlaybackCoordinator / MvPlaybackController | 页面播放会话 |
| 音乐队列与音乐播放器 | GlobalMusicPlayer | 进程 |
| ExoPlayer、Surface、缓存、系统会话 | core:player | 按池、输出和单例分别管理 |

书签保存作品 key、列表索引、毫秒位置与播放意图，不保存 Player 或媒体地址。恢复时有 key 就持续加载后续页，直到命中或到达末页；仅无 key 或确认目标不存在时使用索引回退。查找面板展示已加载数量，追加失败可重试，用户也可主动从当前列表开始；刷新成功通过 revision 重置到首项，失败保留旧列表。追加失败可重试，数据集有限，不制造无限重复项。

音乐实际作品 ID 变化时，ObserveFeedPager 会暂存全局切歌目标，滚动落定后再同步到对应页面；MV 起播与滑动中断通过暂停原因区分恢复。切换协调器有 JVM 单测覆盖；Pager 拖动同步尚未做设备验证。

## 数据驱动的交互 UI

- 三种模式共用喜欢/收藏按钮：未知状态显示重新读取，读取或保存期间禁用重复操作，只有成功值才显示选中态。失败保留原状态并提供原操作重试；读取失败的默认展示不写入缓存。
- 音乐大厅按真实曲目展示封面、标题、作者、纯音乐/歌词信息；主卡跟随列表中的当前曲目，没有当前曲目时展示首曲试听。没有伪造热度、发布时间或互动数量。
- MV/混合流底部排列媒体类型、作品信息、喜欢/收藏与播放控制，移除评论、分享演示入口。大厅与混合流共用真实歌词弹窗。
- 书签查找、空态与首次加载使用居中面板；刷新和失败提示顺序排列，避免提示覆盖彼此。大厅追加状态只由列表 footer 展示。

本轮交互重做仅执行 `git diff --check`，未执行构建、单元测试或设备布局验证。

## 分工与恢复规则

BrowseViewModel 管列表、分页与模式路由；互动（点赞/收藏的缓存、持久化与失败提示）交给 FeedInteractionCoordinator，大厅音乐入口交给 FeedMusicQueue。FeedPlaybackViewModel 管播放状态入口和各模式书签。两个 ViewModel 不互相持有，由 FeedPagerRoute 传入当前列表。播放器对象不进入 SavedStateHandle。

FeedPlaybackCoordinator 只处理音乐与 MV 的切换、状态转换和暂停原因；MvPlaybackController 管视频池、预备任务与进度采集。它们都通过 FeedPlaybackController 连接页面，共用 PlaybackCommands 操作接口，不为三种模式复制 ViewModel。

滑动暂时离开音乐时记录 SCROLL 暂停，取消滑动可以恢复；切到 MV 时记录 MV 暂停。恢复前核对作品 ID 和 intentVersion，用户或系统随后暂停、切歌后，旧恢复请求失效。页面退出时只恢复滑动造成的临时暂停，不自动恢复为 MV 让位的音乐。

## 监听如何选择

ObserveAsEvents 封装的是“按生命周期收集 Flow 并执行回调”，不负责播放器控制，也不会把状态自动变成一次性事件。

| 需求 | 当前方式 | 原因 |
| --- | --- | --- |
| 导航、提示 | ObserveAsEvents | 回调执行副作用；缓冲和重放由上游决定 |
| 播放状态、进度、歌词 | collectAsStateWithLifecycle | 返回前台需要最新状态，局部组件订阅 |
| Pager 选播和全局切歌同步 | ObserveFeedPager | snapshotFlow 观察滚动，collectLatest 取消过时同步 |
| 页面可见性、播放器释放 | BindFeedLifecycle | RESUMED 与页面可见性共同判断；退出时关闭资源 |
| 底层播放器状态 | 控制器协程收集 | 生命周期属于播放器或业务作用域，不依赖 Compose |

ObserveAsEvents 低于 STARTED 时取消收集，恢复后重新订阅；它不保证不丢事件，也不保证只执行一次。购物车的结算事件已经使用此入口。根导航传入会话 StateFlow，依靠状态重放和导航 singleTop 处理重复回调。

## UI 文件如何组织

源码前缀：`src/main/kotlin/com/lyf/composescaffold/feature/browse/`。presentation 按 Tab 分包：`music/`（音乐大厅）、`mv/`（MV 与混合共用的 Pager 引擎，含 `components/` 叶子与 `playback/` 会话层）、`mixed/`（仅混合流渲染的黑胶卡片族）；三 Tab 共享的 UI 骨架留在 `presentation/` 根，共享业务（ViewModel 与其状态、意图、协作类）收在 `presentation/viewmodel/`。跨 Tab 的全局音乐实现保留在 `playback/global/`，其 Hilt 绑定在 `playback/di/`。

| 文件 | 职责 |
| --- | --- |
| presentation/BrowseScreen.kt | 公共主题、Header 与模式路由 |
| presentation/viewmodel/BrowseViewModel.kt | 瘦编排：列表、分页与模式路由，其余职责下放协作类 |
| presentation/viewmodel/FeedUiState.kt | 列表、加载状态与刷新版本 |
| presentation/viewmodel/FeedIntent.kt | 用户意图定义 |
| presentation/viewmodel/MusicHallPlayback.kt | 大厅播放投影（当前曲目、播放意图与播放阶段） |
| presentation/viewmodel/FeedInteractionCoordinator.kt | 点赞/收藏的缓存、持久化与失败提示协作类 |
| presentation/viewmodel/FeedMusicQueue.kt | 大厅对全局音乐播放器的最小视图 |
| presentation/FeedHeader.kt | 三个模式 Tab 与刷新入口 |
| presentation/FeedStatusOverlay.kt | 查找书签、加载、失败重试与互动错误面板 |
| presentation/FeedInteractionButtons.kt | 读取、保存中与已选中的共享互动按钮 |
| presentation/FeedMediaDescription.kt | 从真实媒体字段生成作品类型说明 |
| presentation/FeedScreenOrientation.kt | 页面可见时请求竖屏 |
| presentation/music/MusicHallRoute.kt | 大厅状态绑定与 Intent 转发 |
| presentation/music/FeedMusicHallContent.kt | 音乐大厅分页列表、标题和曲目行 |
| presentation/music/FeedMusicHallArtwork.kt | 音乐大厅当前曲目卡与状态文案 |
| presentation/mv/FeedPagerRoute.kt | 会话装配、恢复位置、封面预取 |
| presentation/mv/FeedPlaybackLifecycleBridge.kt | 页面可见性、生命周期和屏幕常亮 |
| presentation/mv/ObserveFeedPager.kt | 滚动观察、选播、预备和音乐页面同步 |
| presentation/mv/FeedPager.kt | Pager 页面、稳定 key 和拖动互斥 |
| presentation/mv/FeedItemContent.kt | 按层装配背景、媒体内容、互动、信息和错误遮罩 |
| presentation/mv/FeedItemChrome.kt | 音乐和 MV 共用的互动按钮、作品信息与播控装配 |
| presentation/mv/components/FeedVideoPlayerSurface.kt | 视频尺寸、Surface 与首帧封面 |
| presentation/mv/components/FeedPlaybackControls.kt | 活跃作品的进度订阅和操作按钮 |
| presentation/mv/components/FeedPlaybackOverlay.kt | 缓冲、暂停与错误遮罩 |
| presentation/FeedLyricsDialog.kt | 大厅与混合流共用的真实歌词弹窗 |
| presentation/mixed/FeedMusicVinylCard.kt | 混合流音乐项的黑胶和歌词布局 |
| presentation/mixed/VinylDisc.kt | 黑胶绘制与旋转动画 |
| presentation/mixed/LyricPreview.kt | 真实时间轴高亮或静态歌词展示 |

保留有布局意义的 Box、Row、Column；不为减少括号给每一行创建组件，但函数体超过约 80 行或嵌套超过约 5 层时，拆出有独立语义的私有子组件（同文件）。跨音乐/MV 复用的内容或独立的绘制、动画、副作用拆为 components 叶子。组件保持 internal/private，通过状态和回调连接；业务协作类（互动、音乐队列等）从 ViewModel 拆出独立文件，ViewModel 只做编排，不膨胀。

黑胶旋转在 graphicsLayer 中读取动画值，避免为旋转每帧重组组件。歌词进度只由活跃项订阅；只有真实时间轴才高亮，没有时间戳就静态显示。

## 阅读与修改顺序

先读 BrowseScreen → BrowseViewModel / FeedPagerRoute，再读 FeedPlaybackViewModel → FeedPlaybackCoordinator → MvPlaybackController / GlobalMusicPlayer。只有修改底层播放能力时才进入 core:player。

- 改列表与分页：BrowseViewModel、FeedRepository、LoadableController。
- 改音乐队列：GlobalMusicPlayer 和 MusicQueue。
- 改预备策略：FeedPlaybackPolicy 与 MvPlaybackController。
- 改播放按钮：PlaybackCommands 与 FeedPlaybackControls，共用 core:design 的 MediaSeekBar。
- 改视频画面：FeedVideoPlayerSurface；不要从 UI 自行申请 Player。

## 资源与验证边界

MV 池最多 2 个，音乐另有 1 个池；池容量不等于进程总实例数。两条路径共享工厂与 256 MiB LRU 缓存。缓冲时间和字节数是目标，不代表进程内存上限。音乐允许后台播放，MV 按页面生命周期暂停。

修改测试后按仓库规则运行对应模块的 testDebugUnitTest，并执行 git diff --check。单测使用播放器替身，不证明真实通知、Surface、手势或性能正确；未运行设备和性能测量。
