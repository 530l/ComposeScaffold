# core:player

Android / Compose 的 Media3 封装，不依赖 Feed、音乐队列或业务数据层。先读 `playback/Player.kt` 和 `media/MediaSource.kt`；Media3 类型只出现在模块内部实现中。

包结构：`media/`（媒体描述）、`config/`（播放与缓冲配置）、`playback/`（播放器接口与状态）、`media3/`（实现）、`service/`（前台服务与系统会话）。

| 接口 | 职责 |
| --- | --- |
| `PlayerFactory` | 创建池，配置容量和缓冲 |
| `PlayerPool` | 申请、归还和释放播放器 |
| `Player` | 单媒体播放、暂停、Seek、重试和状态 |
| `VideoOutput` | Compose 视频输出、比例和首帧遮罩状态 |

```kotlin
val pool = factory.createPool(capacity = 1)
val player = pool.acquire(
    source = MediaSource(id = "preview", uri = mediaUri, kind = MediaKind.VIDEO),
    options = PlaybackOptions(repeatOne = true),
)
player.setPlaying(true)
// 暂停和销毁由宿主生命周期驱动。
player.setPlaying(false)
pool.recycle(player)
pool.release()
```

`player.video?.Render(modifier)` 显示视频。封面在 `covered` 为 false 后撤下；状态通过 `state` 收集，进度通过 `position` 按需读取，避免播放器强制刷新整个页面。`wantsPlay` 是播放意图，`isPlaying` 是实际输出。

所有池和播放器操作在主线程执行。调用方负责取消未完成的申请，归还后不再使用播放器；释放池前应取消其申请任务。缓存初始化由工厂切到 IO。工厂及 `MediaSessionOwner` 按进程单例使用，避免重复打开同一缓存目录。

`app/di/PlayerModule.kt` 注入公共 OkHttp 客户端、IO 调度器和系统会话属主。缓存默认 256 MiB，可在工厂构造时调整；缓冲按池配置，循环和后台播放按租约配置。默认不开启后台播放，宿主仍需在离屏时暂停。开启后台播放后使用模块内前台服务。

池容量约束单个池；多个池通过共享系统会话交接播放权。业务仍负责列表预备方向、错误降级、队列、书签和生命周期。本模块没有加入转发 UseCase 或通用业务策略框架。

本地 `file` / `content` 等 URI 由 DefaultDataSource 分发，HTTP 使用注入的 OkHttp。后台服务启动失败会暂停播放，`PlayerState.backgroundStartFailed` 为 true，`errorCode` 为 -2；再次播放会重新申请后台能力。切到前台租约时解绑并停止服务，无属主时释放系统会话。

无扩展名的 HLS 地址可设置 `MediaSource.mimeType = "application/x-mpegURL"`，未指定时仍按 URI 推断。普通 Seek 保留现有画面；媒体或 Surface 切换才等待对应首帧。`outputVersion` 仅用于诊断，不能代替 `VideoOutput.covered`。
