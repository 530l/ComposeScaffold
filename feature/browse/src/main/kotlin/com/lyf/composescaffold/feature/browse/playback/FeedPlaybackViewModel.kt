package com.lyf.composescaffold.feature.browse.playback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lyf.composescaffold.core.data.music.MusicPlayerController
import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMode
import com.lyf.composescaffold.core.model.music.toMusicTrack
import com.lyf.composescaffold.core.player.playback.PlayerFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 页面播放状态和书签入口；具体播放与切换由协调器执行。 */
@HiltViewModel
internal class FeedPlaybackViewModel @Inject constructor(
    playerFactory: PlayerFactory,
    val policy: FeedPlaybackPolicy,
    musicController: MusicPlayerController,
    private val savedState: SavedStateHandle,
) : ViewModel(), FeedPlaybackController {
    private var mode: FeedMode? = null
    private var items: () -> List<FeedItem> = { emptyList() }
    private val coordinator = FeedPlaybackCoordinator(
        playerFactory, policy, viewModelScope,
        onCheckpoint = ::rememberCheckpoint,
        musicController = musicController,
    )
    override val state = coordinator.state
    override val progress = coordinator.progress
    override val outputs = coordinator.outputs

    /** 同模式重新绑定不重置作品；模式切换先保存旧书签。 */
    fun bind(mode: FeedMode, items: () -> List<FeedItem> = { emptyList() }): FeedPlaybackController {
        if (this.mode != mode) coordinator.reset()
        this.mode = mode
        this.items = items
        coordinator.musicQueueProvider = { this.items().mapNotNull { it.toMusicTrack() } }
        return this
    }

    fun bookmark(mode: FeedMode): FeedBookmark = FeedBookmark(
        index = savedState["feed.${mode.name}.index"] ?: 0,
        key = savedState["feed.${mode.name}.key"],
        positionMs = savedState["feed.${mode.name}.position"] ?: 0L,
        playWhenReady = savedState["feed.${mode.name}.play"] ?: true,
    )

    private fun rememberCheckpoint(checkpoint: PlaybackCheckpoint) {
        val mode = mode ?: return
        val index = items().indexOfFirst { it.key == checkpoint.key }
        if (index < 0) return
        val prefix = "feed.${mode.name}"
        savedState["$prefix.index"] = index
        savedState["$prefix.key"] = checkpoint.key
        savedState["$prefix.position"] = checkpoint.positionMs
        savedState["$prefix.play"] = checkpoint.wantsPlay
    }

    override fun select(item: FeedItem, positionMs: Long, playWhenReady: Boolean) =
        coordinator.select(item, positionMs, playWhenReady)
    override fun preload(item: FeedItem?) = coordinator.preload(item)
    override fun setHostVisible(visible: Boolean) = coordinator.setHostVisible(visible)
    override fun setPrimarilyVisible(visible: Boolean) = coordinator.setPrimarilyVisible(visible)
    override fun toggle() = coordinator.toggle()
    override fun seekTo(positionMs: Long) = coordinator.seekTo(positionMs)
    override fun retry() = coordinator.retry()
    override fun reset() = coordinator.reset()
    override fun close() = coordinator.close()

    override fun onCleared() = close()
}
