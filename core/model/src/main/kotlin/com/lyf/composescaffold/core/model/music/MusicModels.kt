package com.lyf.composescaffold.core.model.music

import kotlin.random.Random

/** 全局音乐播放核心领域模型集（MusicModels）。 */

/** 单曲音频元数据模型。 */
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val url: String,
    val durationMs: Long = 0L,
)

/** 播单循环播放模式枚举 */
enum class PlayMode {
    LIST_LOOP,
    SINGLE_LOOP,
    SHUFFLE,
}

/** 播放器状态枚举 */
enum class MusicPlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR,
}

/** 播放器实时状态快照模型。 */
data class MusicPlaybackSnapshot(
    val currentTrack: MusicTrack? = null,
    val status: MusicPlaybackStatus = MusicPlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val errorCode: Int? = null,
    val wantsPlay: Boolean = false,
    val seekable: Boolean = false,
    /** 播放意图修改编号；用于使过期的自动恢复失效。 */
    val intentVersion: Long = 0,
) {
    /** 是否正在播放 */
    val isPlaying: Boolean get() = status == MusicPlaybackStatus.PLAYING
    /** 是否正在缓冲等待数据 */
    val isBuffering: Boolean get() = status == MusicPlaybackStatus.BUFFERING
}

/** 待播列表（播单）状态容器与切歌决策状态机。 */
data class MusicQueue(
    val tracks: List<MusicTrack> = emptyList(),
    val currentIndex: Int = -1,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
) {
    /** 当前播放音轨，下标越界时返回 null */
    val currentTrack: MusicTrack? get() = tracks.getOrNull(currentIndex)
    /** 播单是否为空 */
    val isEmpty: Boolean get() = tracks.isEmpty()

    /** 根据当前播放模式智能推导下一首的歌曲索引。 */
    fun nextIndex(): Int? {
        if (tracks.isEmpty()) return null
        return when (playMode) {
            PlayMode.SINGLE_LOOP -> currentIndex.takeIf { it in tracks.indices } ?: 0
            PlayMode.LIST_LOOP -> (currentIndex + 1) % tracks.size
            PlayMode.SHUFFLE -> {
                if (tracks.size == 1) 0
                else {
                    var candidate: Int
                    do {
                        candidate = Random.nextInt(tracks.size)
                    } while (candidate == currentIndex && tracks.size > 1)
                    candidate
                }
            }
        }
    }

    /** 根据当前播放模式推导上一首的歌曲索引。 */
    fun previousIndex(): Int? {
        if (tracks.isEmpty()) return null
        return when (playMode) {
            PlayMode.SINGLE_LOOP -> currentIndex.takeIf { it in tracks.indices } ?: 0
            PlayMode.LIST_LOOP -> (currentIndex - 1 + tracks.size) % tracks.size
            PlayMode.SHUFFLE -> {
                if (tracks.size == 1) 0
                else Random.nextInt(tracks.size)
            }
        }
    }

    /** 从播单中移除指定 ID 的曲目，并安全修正当前播放游标。 */
    fun remove(trackId: String): MusicQueue? {
        val targetIdx = tracks.indexOfFirst { it.id == trackId }
        if (targetIdx == -1) return this
        val newList = tracks.toMutableList().also { it.removeAt(targetIdx) }
        if (newList.isEmpty()) return null
        val newCurrent = when {
            targetIdx < currentIndex -> currentIndex - 1
            targetIdx == currentIndex -> targetIdx.coerceAtMost(newList.lastIndex)
            else -> currentIndex
        }
        return copy(tracks = newList, currentIndex = newCurrent)
    }
}

/** 将 Feed 业务实体转换为全局音乐 [MusicTrack] 实体。 */
fun com.lyf.composescaffold.core.model.feed.FeedItem.toMusicTrack(): MusicTrack? {
    val music = media as? com.lyf.composescaffold.core.model.feed.FeedMedia.Music ?: return null
    return MusicTrack(
        id = key,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
        url = music.url,
    )
}
