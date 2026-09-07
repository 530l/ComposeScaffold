# Compose 音乐 / MV Feed

`feature:browse` 的「逛」默认混合流：MV / 混合使用 `VerticalPager`，纯音乐使用可分页的音乐大厅。保留 `BrowseRoute.Main`、底部五 Tab 及原有返回栈。每种模式有独立的 `LoadableController`、列表、加载状态和播放书签；播放进度不放进列表 UiState。

通用 Media3 封装位于 [core:player](../core/player/README.md)，不依赖 Feed 模型；app 注入公共 HTTP 客户端。循环、后台播放、池容量与缓冲由调用方配置。

代码入口和调用关系见 [Feed 代码导读](../feature/browse/src/main/kotlin/com/lyf/composescaffold/feature/browse/DESIGN.md)。

## 数据与采样结果

2026-09-06 通过 Android Studio 内嵌模拟器运行 museai，使用电脑控制操作推荐流、资产页和 Logcat。参考工程：`/Users/a530/Desktop/xingmeng/android/museai`。

| 来源 | 完整记录 | 实际接入 |
| --- | --- | --- |
| 公开推荐流 | 297 条，包括 288 首歌曲、9 条 MV | 按采样顺序选取 30 首歌曲及全部 9 条 MV，随仓库提供 |
| 资产 MV 详情 | 4 条竖屏 MV | 仅加入忽略提交的本地样本 |

公开 MV 为 5 条竖屏（包括 1 条 `orientation = 9:16`）、4 条横屏。原定至少 10 条竖屏、10 条横屏 MV 的采样数量尚未达到，没有复制作品补数。当前本地数据共 43 条，仓库默认数据为 39 条。歌词字段按原样保留；没有真实时间戳就静态展示，没有歌词就显示占位。

推荐链路：`ExploreV4Repository → getV3ExploreRecommendPage → /project/song/v30/explore/recommend/page`。资产播放详情来自 `/project/song/v30/mv/info`，其对象没有推荐流的 `workType`，依据 MV 接口明确映射为 `MV`，作者由 `authorName` 映射为 `userName`。不把事件上报中的只有 `workId/workType` 的请求对象当成完整作品。

记录只保留 `workId/uuid`、`workType`、`audioUrl/streamAudioUrl/videoUrl`、`imageUrl`、`orientation`、`title`、`userName`、`lyrics`、`subType`。MV 必须使用 `videoUrl`，不能优先使用音频试听地址。

- 默认资产：`core/data/src/main/assets/feed/museai-public.json`。仅来自公开推荐接口，已排除带查询参数、片段、用户信息及非 HTTPS 的地址；本次 297 条公开记录没有此类排除项。没有删除签名参数或下载媒体文件。
- 开发覆盖：`.local/feed-assets/feed/museai.json`，JSON 数组，字段与默认资产相同。通过 AGP Variant Sources API 仅合入 debug 资产；Repository 还要求 `AppEnvironment.DEVELOPMENT`。资产作品和任何签名地址均只放此目录，不进入 release 或提交。
- 完整白名单采集记录保留在本机 `.local/feed-public-capture.json` 和 `.local/feed-asset-capture.json`，不保留完整响应、账号信息或无关日志。
- 网络媒体仍依赖原站可用性；本次未逐条发请求验证地址有效期，也未获取素材再分发许可。示例只引用作品地址，正式发布需另行确定素材来源。

继续采样时在 Android Studio Logcat 限定 `package:com.xingchat.muse tag:AtmobApi`，用较短的 `age` 窗口避免编辑器截断。日志中的 `│ ` 是包装前缀；去掉前缀后将折行直接拼接，按完整 JSON 对象边界解析，丢弃截断对象，再按上述字段白名单输出本地文件。资产详情没有 `workType`，必须先确认接口再映射。不要导出或粘贴整个 Logcat，也不要将只有事件标识的对象覆盖完整详情。

## 分页与交互

Repository 使用 1 基页码，先过滤完整集合再分页；混合流保留样本顺序。每页 10 条，模拟延迟 400ms；剩余 3 条时通过滚动观察发出 Intent，复用现有分页控制器加载下一页。有限数据集正确结束，不循环追加副本。Pager key 为类型与作品 ID 的组合。

顶部刷新成功后回到第一条；失败保留原列表、浏览位置与分页状态。追加失败不会停止当前作品，页面显示重试入口。本地喜欢、收藏通过非敏感 KeyValueStore 保存，保存失败显示提示。评论和分享明确显示演示弹窗。

开发包可在 `.local/feed-assets/feed/scenario.txt` 写入以下一个值，重新打包并重启进程后生效；默认和 release 都为 `NORMAL`：

| 值 | 行为 |
| --- | --- |
| `INITIAL_FAILURE` | 每个模式首次第 1 页失败一次，可重试 |
| `APPEND_FAILURE` | 每个模式首次第 2 页失败一次，可重试 |
| `DUPLICATE_PAGE` | 第 2 页重复第 1 页，此后继续剩余数据，检验去重空页仍可分页 |
| `MEDIA_FAILURE` | 媒体地址替换为保留的无效示例域名，封面保留，可继续滑动 |

## 播放与资源边界

所有 Media3 组件统一为 `1.11.0`，包含 ExoPlayer、common-ktx、Compose UI、OkHttp 数据源、HLS 和数据库组件。版本与 API 参见 [Media3 版本说明](https://developer.android.com/jetpack/androidx/releases/media3?hl=en) 和 [Compose UI 文档](https://developer.android.com/media/media3/ui/compose)。

- 协调器在主线程管理官方 `PlayerPool`，仅服务 MV：当前作品和一个方向预备项，通常最多 2 个实例。预组合页面只能读取租约，不自行创建播放器。切换或取消时用代次、作品 ID 与租约身份丢弃旧请求。音乐不占用页面级播放器，统一交给全局播放器发声。
- `targetPage` 决定预备项，`settledPage` 决定播放作品；当前页失去主要可见区域就暂停。先暂停其他项，再启动当前项。中途取消滚动保留原播放意图。
- MV 默认单条循环，音乐按全局队列的循环模式播放。切换新作品从 0 开始；切模式、切 Tab、退出再进入恢复书签，仍由全局播放器播放的同一首音乐保留当前进度。书签只保存标识、位置、进度及播放意图，不序列化播放器或媒体地址。
- `PlayerSurface` 默认 SurfaceView 输出，封面只在同一媒体和实际 Surface 的首帧事件后撤下。视频先依据方向预留 9:16 / 16:9，再采用真实视频尺寸和像素宽高比校正，完整显示。周围使用小尺寸封面与渐变。
- 每播放器最大缓冲时间 10 秒、目标字节阈值 16 MiB，优先字节阈值；这不是进程内存上限。低内存设备及解码错误降为单播放器，当前会话不再恢复双实例。
- 音乐独占一个播放器，MV 池最多两个；系统会话使用不创建解码器的 `SimpleBasePlayer` 占位。进程总数不等于 MV 池容量。
- 进程共用一个 256 MiB LRU `SimpleCache`，在 IO 调度器初始化；预备和正式播放共用缓存。封面只预取附近作品，不增加独立媒体预加载器。
- MV 进度每 250ms 轮询（协调器常量 `PROGRESS_INTERVAL_MS`，可构造注入），暂停时停止；音乐进度镜像全局播放器的 250ms 状态快照。拖动仅本地预览，松手提交 Seek。缓冲超过 300ms 显示提示；失败保留封面并显示错误码和重试。
- 页面可见且生命周期处于 `RESUMED` 才允许 MV 播放。MV 退后台固定延时 10 秒（`TEARDOWN_DELAY_MS`）后回收播放器池，期限内回到前台取消回收并无缝恢复；音乐不参与延时回收。手动暂停、耳机断开及永久音频焦点丢失后的暂停意图保留。
- 音乐由进程级单例 `GlobalMusicPlayer`（实现 core:data 的 `MusicPlayerController` 契约）承载：Feed 选中音乐时把当前列表映射为播单交其接管，全局迷你播放器（`GlobalMiniPlayer`）、系统媒体通知与锁屏播控随之可用；迷你播放器切歌后，混合流按实际作品 ID 同步 Pager 和进度。MV 起播前先暂停全局音乐，避免双声道并行。
- 前台服务 `MediaPlaybackService`（Media3 `MediaSessionService`）在 `startForegroundService` 拉起后立即以占位通知进入前台履行系统时限义务（本应用 UI 不经 MediaController 连接，会话由服务显式挂入），Media3 通知就绪后接管展示；切换为前台 MV 或清空当前音乐时解绑并停止服务；无播放属主时释放系统会话。
- 页面可见时请求竖屏，离开恢复宿主原设置；大屏或系统忽略方向请求时仍按实际约束布局。底栏空间由既有 Tab 容器让出，Feed 不重复扣除底部 Insets。
- 图片和媒体复用 `@PublicHttpClient`，不携带业务认证信息。`FeedPlayback` 日志只记录脱敏作品标识、输出耗时、缓冲次数、掉帧、错误码和租约数。

## 验证边界

2026-09-07 运行 `:core:player:testDebugUnitTest`、`:feature:browse:testDebugUnitTest` 和 `:app:testDebugUnitTest`：共 37 项通过，0 失败；对应 Debug 代码编译通过。新增覆盖缓冲暂停、迟到请求、错误恢复、音乐身份与书签、MV 外部播控、模式隔离和 Seek 边界。`git diff --check` 通过。

这些是 JVM 回归验证，未执行设备播放、手势、通知栏和性能验收。测试修改时运行对应模块单测已由 AGENTS.md 授权；没有运行完整 Test Plan、Detekt 或自动格式化。

保留但未执行的验收场景：音乐与 MV 各种切换、快速连滑、反向与取消；横竖屏及方向缺失、首帧等待、无歌词、不可 Seek、媒体失败；分页互斥、重试、刷新失败、重复页、末页、模式隔离；切 Tab、登录覆盖、后台、锁屏、耳机断开、恢复状态，以及长时间播放下的实例与缓存边界。

真机调优目标仍为预备命中时切换耗时 P95 ≤ 200ms、连续滑动卡顿帧比例 < 1%。这些是后续在固定设备、网络与素材下测量的目标，不代表当前已达到。
