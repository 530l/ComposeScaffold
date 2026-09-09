package com.lyf.composescaffold.feature.browse.presentation.mv.playback

/** Feed 播放书签（断点记忆）。 */
internal data class FeedBookmark(
    val index: Int = 0, // 备用索引：key 在列表中找不到时退回按索引定位。
    val key: String? = null, // 优先用稳定作品标识找回原条目。
    val positionMs: Long = 0,
    val playWhenReady: Boolean = true, // 保留手动暂停选择，恢复后不强制自动播。
)
