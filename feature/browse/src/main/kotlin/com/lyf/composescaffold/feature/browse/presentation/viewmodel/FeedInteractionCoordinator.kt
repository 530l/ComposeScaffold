package com.lyf.composescaffold.feature.browse.presentation.viewmodel

import com.lyf.composescaffold.core.data.repository.FeedInteraction
import com.lyf.composescaffold.core.data.repository.FeedInteractionStore
import com.lyf.composescaffold.core.model.feed.FeedItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class FeedInteractionOperation { READ, LIKE, SAVE }

/** 失败携带原操作，重试时只处理对应作品。 */
internal data class FeedInteractionFailure(
    val key: String,
    val operation: FeedInteractionOperation,
)

/** 只缓存成功读取或写入的互动；未知状态不能当作未选中状态落盘。 */
internal class FeedInteractionCoordinator(
    private val store: FeedInteractionStore,
    private val scope: CoroutineScope,
) {
    private val _interactions = MutableStateFlow<Map<String, FeedInteraction>>(emptyMap())
    val interactions = _interactions.asStateFlow()
    private val _pending = MutableStateFlow<Set<String>>(emptySet())
    val pending = _pending.asStateFlow()
    private val _error = MutableStateFlow<FeedInteractionFailure?>(null)
    val error = _error.asStateFlow()
    private val mutex = Mutex()

    suspend fun prime(items: List<FeedItem>) {
        items.forEach { item ->
            if (item.key !in _interactions.value && item.key !in _pending.value) {
                _pending.update { it + item.key }
                perform(item.key, FeedInteractionOperation.READ)
            }
        }
    }

    fun read(key: String) = request(key, FeedInteractionOperation.READ)

    fun toggle(key: String, like: Boolean) = request(
        key,
        if (like) FeedInteractionOperation.LIKE else FeedInteractionOperation.SAVE,
    )

    fun retry() {
        val failure = _error.value ?: return
        request(failure.key, failure.operation)
    }

    fun dismissError() {
        _error.value = null
    }

    private fun request(key: String, operation: FeedInteractionOperation) {
        // 入口在 UI 线程同步占位，阻止同一作品在保存期间重复排队切换。
        if (key in _pending.value) return
        _pending.update { it + key }
        scope.launch { perform(key, operation) }
    }

    private suspend fun perform(key: String, operation: FeedInteractionOperation) {
        try {
            mutex.withLock {
                // 读取失败不发布默认值；下次操作仍须取得真实状态。
                val current = _interactions.value[key] ?: store.read(key).also { value ->
                    _interactions.update { it + (key to value) }
                }
                val next = when (operation) {
                    FeedInteractionOperation.READ -> current
                    FeedInteractionOperation.LIKE -> current.copy(liked = !current.liked)
                    FeedInteractionOperation.SAVE -> current.copy(saved = !current.saved)
                }
                if (operation == FeedInteractionOperation.READ || store.write(key, next)) {
                    _interactions.update { it + (key to next) }
                    // 其他作品的错误不能被本次成功吞掉。
                    _error.update { if (it?.key == key) null else it }
                } else {
                    _error.value = FeedInteractionFailure(key, operation)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            _error.value = FeedInteractionFailure(key, operation)
        } finally {
            _pending.update { it - key }
        }
    }
}
