package com.lyf.composescaffold.feature.browse.presentation.viewmodel

/** 大厅仅订阅曲目和播放阶段，进度变化不触发列表重组。 */
internal data class MusicHallPlayback(
    val trackId: String? = null,
    val isPlaying: Boolean = false,
    val wantsPlay: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
)
