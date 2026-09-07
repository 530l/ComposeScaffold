package com.lyf.composescaffold.core.data.repository

import android.content.Context
import com.lyf.composescaffold.core.common.config.AppConfig
import com.lyf.composescaffold.core.common.config.AppEnvironment
import com.lyf.composescaffold.core.data.coroutine.IoDispatcher
import com.lyf.composescaffold.core.data.storage.KeyValueStore
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.feed.FeedPage
import com.lyf.composescaffold.core.model.feed.FeedSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * 流媒体作品数据仓库抽象契约（FeedRepository）。
 *
 * 【作用与职责】：
 * 隔离数据源（远程接口/本地样本/缓存），向上层业务提供按流媒体分类模式（纯音乐/短视频MV/混合流）分页加载作品列表的统一接口。
 * 遵循 1 基页码规范，内部负责过滤、切片与错误封装。
 *
 * 【核心依赖】：
 * - [FeedMode]：流媒体分类筛选枚举（MUSIC/MV/MIXED）。
 * - [FeedPage]：分页数据容器（包含当前页作品列表、是否有更多页、数据源类型）。
 *
 * 【提供功能】：
 * - [loadPage]：按分类模式分页获取流媒体作品列表。
 */
interface FeedRepository {
    /**
     * 分页加载 Feed 流数据。
     * 页码从 1 开始；分类模式过滤发生在分页切片之前。
     *
     * @param mode 筛选分类模式
     * @param page 请求页码（>= 1）
     * @param pageSize 单页容量
     * @return 分页结果包装对象 [Result]
     */
    suspend fun loadPage(mode: FeedMode, page: Int, pageSize: Int): Result<FeedPage>
}

/** 开发联调与测试场景枚举，支持模拟弱网失败、末页、空列表与媒体失效等边缘情况。 */
private enum class FeedScenario {
    NORMAL, // 正常流程
    INITIAL_FAILURE, // 首次首屏加载失败
    APPEND_FAILURE, // 翻页上拉加载失败
    DUPLICATE_PAGE, // 返回重复数据页（测试客户端去重能力）
    MEDIA_FAILURE, // 媒体直链失效（测试播放器容灾）
}

/**
 * 本地资产/模拟样本流媒体数据仓库实现（SampleFeedRepository）。
 *
 * 【作用与职责】：
 * 读取 assets/feed/ 下的静态样本 JSON 数据，经解析器 [FeedSampleParser] 构建内存目录索引。
 * 在指定 IO 调度器上执行线程安全的分类过滤、分页切片、去重与模拟网络延迟，支撑无后端网络环境下的全真演示。
 *
 * 【核心依赖】：
 * - [Context]：读取 assets 静态资源。
 * - [IoDispatcher]：后台 IO 协程调度器。
 * - [AppConfig]：应用运行环境配置（判断开发联调模式）。
 * - [Json]：kotlinx.serialization JSON 解析器。
 *
 * 【提供功能】：
 * - 线程安全的样本列表懒加载与内存目录缓存；
 * - 纯音乐与短视频流的模式过滤与分页切片；
 * - 开发环境下的故障注入模拟。
 */
@Singleton
class SampleFeedRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val config: AppConfig,
    private val json: Json,
) : FeedRepository {
    private val mutex = Mutex() // 线程安全互斥锁
    private var catalog: List<FeedItem>? = null // 内存目录缓存
    private var source = FeedSource.MUSEAI_PUBLIC // 当前数据源标识
    private var scenario = FeedScenario.NORMAL // 故障模拟场景
    private val failed = mutableSetOf<Pair<FeedMode, Int>>() // 已触发模拟失败的记录集合

    override suspend fun loadPage(mode: FeedMode, page: Int, pageSize: Int): Result<FeedPage> =
        withContext(ioDispatcher) {
            try {
                require(page > 0 && pageSize in 1..100) { "分页参数无效" } // 校验入参范围
                delay(400L.milliseconds) // 模拟真实网络往返延迟
                mutex.withLock {
                    // 1. 获取全部样本并按模式过滤
                    val all = loadCatalog().filter {
                        mode == FeedMode.MIXED ||
                            (mode == FeedMode.MUSIC && it.media is FeedMedia.Music) ||
                            (mode == FeedMode.MV && it.media is FeedMedia.Mv)
                    }
                    // 2. 模拟开发环境特定页失败场景
                    if (((scenario == FeedScenario.INITIAL_FAILURE && page == 1) ||
                            (scenario == FeedScenario.APPEND_FAILURE && page == 2)) &&
                        failed.add(mode to page)
                    ) {
                        throw IOException("开发场景模拟加载失败")
                    }
                    // 3. 插入重复页测试去重能力
                    val sourcePage =
                        if (scenario == FeedScenario.DUPLICATE_PAGE && page >= 2) page - 1 else page
                    // 4. 计算切片起始与结束下标
                    val start =
                        ((sourcePage.toLong() - 1) * pageSize).coerceAtMost(all.size.toLong())
                            .toInt()
                    val end = (start + pageSize).coerceAtMost(all.size)
                    // 5. 提取本页作品并处理失效媒体模拟
                    val items = all.subList(start, end).map { item ->
                        if (scenario != FeedScenario.MEDIA_FAILURE) item else item.copy(
                            media = when (val media = item.media) {
                                is FeedMedia.Music -> media.copy(url = "https://invalid.example/feed.mp3")
                                is FeedMedia.Mv -> media.copy(url = "https://invalid.example/feed.mp4")
                            },
                        )
                    }
                    Result.success(FeedPage(items, end < all.size, source))
                }
            } catch (error: CancellationException) {
                throw error // 保留协程取消语义，不转换为普通失败
            } catch (_: Exception) {
                // 不将解析异常原文传给 UI/日志，原文可能含采样 URL
                Result.failure(IOException("Feed 样本加载失败"))
            }
        }

    /**
     * 懒加载并解析本地 assets 目录中的 JSON 样本数据。
     */
    private fun loadCatalog(): List<FeedItem> {
        catalog?.let { return it } // 命中美存直接返回
        val development = config.environment == AppEnvironment.DEVELOPMENT
        val assets = context.assets.list("feed").orEmpty()
        val local = development && "museai.json" in assets
        val file = if (local) "museai.json" else "museai-public.json"
        val text = context.assets.open("feed/$file").bufferedReader().use { it.readText() }
        val items = json.parseToJsonElement(text).jsonArray.mapNotNull(::parseFeedSample)
            .distinctBy { it.key } // 按唯一 Key 客户端去重
        require(items.isNotEmpty()) { "样本未包含可播放内容" }
        if (development && "scenario.txt" in assets) {
            val name = context.assets.open("feed/scenario.txt").bufferedReader()
                .use { it.readText().trim() }
            scenario = FeedScenario.entries.firstOrNull { it.name == name } ?: FeedScenario.NORMAL
        }
        source = if (local) FeedSource.MUSEAI_LOCAL else FeedSource.MUSEAI_PUBLIC
        catalog = items
        return items
    }
}

/**
 * 作品互动状态模型（点赞、收藏）。
 *
 * @param liked 是否已点赞
 * @param saved 是否已收藏
 */
data class FeedInteraction(
    val liked: Boolean = false,
    val saved: Boolean = false,
)

/**
 * 作品点赞与收藏状态持久化存储契约（FeedInteractionStore）。
 *
 * 【作用与职责】：
 * 负责短视频/流媒体条目的本地交互标记（点赞、收藏）的读写持久化，支持未来与云端同步。
 *
 * 【核心依赖】：
 * - [FeedInteraction]：点赞与收藏数据载体。
 *
 * 【提供功能】：
 * - [read]：读取指定作品的互动状态；
 * - [write]：写入指定作品的互动状态。
 */
interface FeedInteractionStore {
    /** 读取指定作品 Key 的互动状态 */
    suspend fun read(key: String): FeedInteraction
    /** 写入指定作品 Key 的互动状态，返回是否持久化成功 */
    suspend fun write(key: String, value: FeedInteraction): Boolean
}

/**
 * 基于 KeyValueStore 的轻量本地互动状态持久化实现（DefaultFeedInteractionStore）。
 *
 * 【作用与职责】：
 * 使用位掩码（Bitmask）高效压缩存储点赞与收藏状态（bit 0 表示点赞，bit 1 表示收藏），
 * 在 IO 调度器上安全完成读写，写入失败时如实返回 false。
 *
 * 【核心依赖】：
 * - [KeyValueStore]：底层偏好与缓存键值存储接口。
 * - [IoDispatcher]：IO 调度器。
 *
 * 【提供功能】：
 * - 位压缩读取与写入点赞、收藏状态。
 */
@Singleton
class DefaultFeedInteractionStore @Inject constructor(
    private val store: KeyValueStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FeedInteractionStore {
    override suspend fun read(key: String): FeedInteraction = withContext(ioDispatcher) {
        val flags = store.getInt("feed.interaction.$key") // 读取整型位掩码
        FeedInteraction(liked = flags and 1 != 0, saved = flags and 2 != 0) // 解码位标记
    }

    override suspend fun write(key: String, value: FeedInteraction): Boolean = withContext(ioDispatcher) {
        store.putInt(
            "feed.interaction.$key",
            (if (value.liked) 1 else 0) or (if (value.saved) 2 else 0), // 编码位标记并持久化
        )
    }
}
