package com.lyf.composescaffold.feature.browse.playback

/** Feed 播放书签（断点记忆）。 */
internal data class FeedBookmark(
    val index: Int = 0,
    val key: String? = null,
    val positionMs: Long = 0,
    val playWhenReady: Boolean = true,
)
