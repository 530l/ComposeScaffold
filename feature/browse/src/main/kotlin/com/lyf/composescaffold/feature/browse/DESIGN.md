# Feed 代码导读

完整设计（整体调用关系图、音乐与 MV 播放链路、状态归属表）在[模块 README](../../../../../../../../../README.md)，本页只做一件事：**带你按正确的心智模型把代码读顺**。

本目录的 Kotlin 注释有主见：只解释语义、设计取舍、约束与陷阱（面向首次进入本模块的同事），不复述代码字面；导入、注解与独立括号不注释。

## Compose 函数导览

presentation 层函数多，但只分四类。先认类别，再进调用树，就不会糊。

| 类别 | 识别特征 | 函数 |
| --- | --- | --- |
| A 装配层 | 名字带 Route；全模块只有它们持有 ViewModel | BrowseScreen、MusicHallRoute、FeedPagerRoute |
| B 副作用层 | Observe/Bind/Keep/Prefetch 前缀；不画任何 UI，全是 LaunchedEffect/DisposableEffect | ObserveFeedPager、BindFeedLifecycle、KeepFeedPortrait、PrefetchAppImages |
| C 布局容器层 | 只吃参数、吐画面，不接触 ViewModel | FeedHeader、FeedPager、FeedItemContent、FeedItemInfo、FeedMusicHallContent、FeedStatusOverlay |
| D 叶子组件 | 播放叶子在 mv/components/ 与 mixed/，共享互动及歌词在 presentation/ | VinylDisc、LyricPreview、FeedPlaybackControls、FeedVideoPlayerSurface、FeedMusicVinylCard、FeedPlaybackOverlay、FeedLyricsDialog |

B 类最容易误导：签名是 @Composable 却一行 UI 都不画。例如 ObserveFeedPager 整个函数只有三个 LaunchedEffect，它是“滚动 → 选播/预备/翻页”的驱动器，不是画面。

### 调用树

```text
BrowseEntryProvider (navigation/)          注册导航入口
└─ BrowseScreen                            ★ 门面：唯一创建两个 ViewModel 的地方
   ├─ KeepFeedPortrait                     锁竖屏（B 类，无 UI）
   ├─ FeedHeader                           三个模式 Tab + 刷新
   └─ key(mode) 按模式二选一
      ├─ 音乐 → MusicHallRoute（presentation/music/）
      │   ├─ FeedMusicHallContent          分页列表 + 曲目行
      │   │   └─ MusicHallBanner           当前曲目封面与真实播放阶段
      │   └─ FeedStatusOverlay             加载/失败/重试提示
      └─ MV/混合 → FeedPagerRoute（presentation/mv/）★★ 全模块最复杂的函数
          ├─ playbackOwner.bind(mode){最新列表} → session
          │                                    播放会话（就是 FeedPlaybackViewModel 自己）
          ├─ BindFeedLifecycle             前后台可见性/常亮/退出 close（B 类）
          ├─ PrefetchAppImages             预取相邻封面（B 类）
          ├─ LaunchedEffect                书签定位：数据不够继续 LoadMore
          ├─ ObserveFeedPager              滚动 → 选播/预备/提前翻页（B 类）★
          └─ Box
              ├─ FeedPager                 VerticalPager（beyondViewportPageCount = 1）
              │   └─ 每页 FeedItemContent  单条作品全屏组装
              │       ├─ 96px 封面铺底 + 黑色遮罩       背景
              │       ├─ 整页点击 = toggle（仅 active 项响应）
              │       ├─ 音乐 → FeedMusicVinylCard → VinylDisc + LyricPreview
              │       ├─ MV → FeedVideoPlayerSurface
              │       ├─ FeedItemInfo                  底部：媒体类型/标题/作者/真实互动 + 播控
              │       │   └─ FeedPlaybackControls      订阅 250ms 进度 + MediaSeekBar
              │       ├─ FeedPlaybackOverlay           缓冲/暂停/错误遮罩
              │       └─ FeedLyricsDialog                    真实歌词弹窗（大厅/混合共用）
              └─ FeedStatusOverlay
```

### 参数里“又有值又有流”不是混乱，是双通道

- `playback: FeedPlaybackState` 是**值**：装配层 collect 一次、层层传下去；低频变化，重组整条 item 可接受。
- `progress: StateFlow<PlaybackProgress>` 是**流**：250ms 高频更新，按引用一路传到真正需要它的叶子（进度条 FeedPlaybackControls、歌词高亮 LyricPreview）内部才 collect，且只有 active 项订阅。在上层图省事 collect 一次，一次 tick 就会把预组合的邻页全部重组。

同理，C/D 层不拿 ViewModel，只拿最小接口 PlaybackCommands（toggle/seekTo/retry，见 FeedPlaybackController.kt）和若干回调，因此可以独立挪动与测试。

### FeedPagerRoute 一个函数干四件正事

1. 按书签 key 持续查找，直到命中或末页；没有 key 才直接按索引恢复。失败可重试，用户可主动跳过；
2. 刷新 revision 变化时先 reset 回到首条；
3. 摆副作用组件（BindFeedLifecycle、ObserveFeedPager、封面预取）；
4. 画 FeedPager 与 FeedStatusOverlay。

第一遍读它只看第 4 件——它摆了哪些子组件；两个 LaunchedEffect 回头再精读。

### 跟读时最容易撞墙的点

1. **三个页码各司其职**：`settledPage`（上次落定页，选播只认它——手势可能取消回弹）、`currentPage`（最靠近吸附位的页，`current == settled` 判定是否仍占主要区域）、`targetPage`（本次滑动预计到达页，预备优先它）。一句话：**选播保守、预备激进**。
2. **restoring 有两个来源、四处消费**：来源是书签恢复和刷新版本变化；消费在生命周期桥（禁起播）、FeedPager（禁手势）、ObserveFeedPager（禁选播）、FeedStatusOverlay（遮画面）。刷新时置 true，查找完成、确认空结果或用户跳过时置 false。
3. **revision 藏在 copyState 里递增**：只在“成功刷新结束且列表引用变化”时 +1（BrowseViewModel 的 copyState），分页追加不触发；Pager 靠作品 key 保持页面稳定，只有下拉刷新才回跳首页。
4. **书签是一次性消费的**：落定选播时 `resume.key == selected.key` 才应用保存的进度/意图，随后立即清空（onResumeConsumed），否则滑到任何新作品都会从书签进度开始。
5. **session 不是新对象**：`bind()` 返回 this，FeedPlaybackViewModel 自己实现了 FeedPlaybackController。`remember(playbackOwner)` 保证同一次组合只 bind 一次；模式切换的“重建”发生在更外层的 `key(mode)`。
6. **active 不是“可见”而是“持有播放权”**：Pager 预组合了相邻页，同屏 2-3 个 FeedItemContent 存活，但 toggle、进度订阅、遮罩全部 gated by `playback.key == item.key`。
7. **video 是渲染句柄不是播放器**：`key(video) { video.Render() }`——VideoOutput 只有 covered/aspectRatio/Render 三样，UI 层完全不知道 ExoPlayer 存在。
8. **covered 三态**：还没拿到输出也当 covered（封面盖着）；渲出首帧才 false 淡出；出错时重新盖住。不能用 READY 或 outputVersion 判断首帧。
9. **wantsPlay 与 isPlaying 各有消费者**：播控按钮图标用 wantsPlay（缓冲中也能暂停）、黑胶旋转用 isPlaying（真出声才转）、暂停遮罩用 !wantsPlay。合成单一布尔必丢语义。
10. **scrubbing 手势仲裁**：进度条拖动状态经 FeedItemActions 回传 FeedPager 临时关闭 userScrollEnabled，防止拖进度引发翻页。
11. **setPrimarilyVisible 每个快照都调**，靠下游（协调器/MV 控制器）“值未变直接 return”的幂等保护。
12. **keepScreenOn 是链式保存-还原**：DisposableEffect 每次依赖变化先还原 previous 再设新值，退出时还原到进入前的值。

## 播放链路速记（配合模块 README）

### MV 从静到响要过六道门

宿主 `playbackVisible`（根路由可见 && 当前 Tab 是逛 && 停在根路由）→ `playbackVisible && !restoring` → `hostVisible`（再 && 页面 RESUMED）→ 滚动落定才 select（滑动中 primarilyVisible=false 压住）→ `policy.shouldPlay(hostVisible && primarilyVisible && wantsPlay)` 三条件与 → 起播前 beforePlay 让全局音乐让位。排查“有画面没声/一直转圈”按此链从外到内逐环检查。音乐路径刻意跳过后三道门：退页只存检查点不停播，这就是后台续播。

### 混合流里音乐/MV 互斥分三层

手势层（滑动一开始 SCROLL 暂停）、协调器层（落定 MV 时升级为 MV 暂停；滑回同一首时不重新 playTrack、沿用全局进度并恢复）、系统会话层（core:player 的 MediaSessionOwner 交接进程唯一的系统会话）。恢复前校验曲目未换且 intentVersion 未变——用户手动暂停过的歌不会被自动恢复。

### 三种版本号治理所有异步竞态

- MV 池的 **generation/ticket**：丢弃迟到的 suspend acquire 结果；
- 全局音乐的 **intentVersion**：任何用户/系统改意图都递增，过期的自动恢复请求对不上号即失效（乐观锁）；
- 前台服务的 **generation**：过期的服务启动失败回调被忽略。
- 系统侧暂停（耳机/焦点）经 **externalIntentVersion** 镜像递增 intentVersion。

### 书签是按模式的单槽位

`feed.{mode}.index/key/position/play` 四个键不区分媒体类型，MIXED 下音乐与 MV 检查点写同一组键，永远只记“最后活跃的作品”；key 必须能在当前列表找到才落盘，别的队列的歌不污染。MV 检查点约每 3.75 秒（15 个进度采样）存一次。

### 两个 ViewModel 的分工与联动

BrowseViewModel 管“看什么”（列表/分页/模式路由，零 core.player 依赖；互动在 FeedInteractionCoordinator，大厅音乐在 FeedMusicQueue），FeedPlaybackViewModel 管“怎么播”（书签/会话，零 FeedRepository 依赖）。不互相持有，仅有的三处联动全经 FeedPagerRoute 中转：列表晚绑定 lambda（bind 传入）、revision 变化 reset、翻页请求 onIntent。

## 阅读顺序

第一遍 UI 层（本页调用树自上而下）：

1. `navigation/BrowseEntryProvider.kt` → `presentation/BrowseScreen.kt`：入口与模式路由。
2. `presentation/viewmodel/`：BrowseViewModel 与同目录的 FeedUiState/FeedIntent/FeedInteractionCoordinator/FeedMusicQueue。
3. `presentation/music/`：音乐大厅三件套（MusicHallRoute → FeedMusicHallContent → FeedMusicHallArtwork）。
4. `presentation/mv/FeedPagerRoute.kt`：先只看结构，跳过 LaunchedEffect。
5. `presentation/mv/FeedPager.kt` → `FeedItemContent.kt`：一屏一项的组装。
6. `presentation/mv/FeedItemChrome.kt` → `presentation/mv/components/` 与 `presentation/mixed/`：互动、信息与播控叶子。
7. 回头精读 FeedPagerRoute 的恢复 LaunchedEffect 与 `presentation/mv/ObserveFeedPager.kt`：最烧脑，放最后。

第二遍播放链路（配合模块 README 的调用关系图）：

1. `presentation/mv/playback/FeedPlaybackViewModel.kt` → `FeedPlaybackCoordinator.kt`：书签与音乐/MV 切换。
2. `presentation/mv/playback/MvPlaybackController.kt` / `playback/global/GlobalMusicPlayer.kt`：视频池与全局音乐队列。
3. 只有修改底层播放能力时才进入 core:player。

注释解释现有行为，不代表设备或性能验收结论。大屏判断依据窗口配置，并非检测折叠硬件。
