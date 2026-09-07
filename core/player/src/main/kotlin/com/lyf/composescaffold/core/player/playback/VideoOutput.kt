package com.lyf.composescaffold.core.player.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/** Compose 视频输出接口，covered 为 false 后撤下封面。 */
interface VideoOutput {
    /** true 表示需要封面遮罩。 */
    val covered: StateFlow<Boolean>

    /** 画面宽高比，获取视频尺寸前使用初始值。 */
    val aspectRatio: StateFlow<Float>

    /** 在 Compose 中显示视频画面。 */
    @Composable fun Render(modifier: Modifier)
}
