package com.lyf.composescaffold.feature.browse.presentation.mv.playback

/** 决定池容量、预备目标和起播条件。 */
internal interface FeedPlaybackPolicy {
    fun capacity(lowRam: Boolean): Int
    // 三个下标含义：settled＝已停落页，current＝当前主要可见页，target＝滑动预测目标页。
    fun neighbor(settled: Int, current: Int, target: Int, itemCount: Int): Int?
    fun shouldPlay(hostVisible: Boolean, primarilyVisible: Boolean, wantsPlay: Boolean): Boolean
}

/** 最多两个播放器，优先预备滑动目标。 */
internal class ShortVideoPlaybackPolicy : FeedPlaybackPolicy {
    /** 低内存设备保留一个播放器，其他设备最多两个。 */
    override fun capacity(lowRam: Boolean): Int = if (lowRam) 1 else 2

    override fun neighbor(settled: Int, current: Int, target: Int, itemCount: Int): Int? {
        val index = when {
            // 已有明确滑动目标时优先预备它。
            target != settled -> target
            // 向前一页滑动时预备上一项。
            current < settled -> settled - 1
            // 正常停留时预备下一项。
            else -> settled + 1
        }
        // 到达列表边界时不返回越界索引。
        return index.takeIf { it in 0 until itemCount }
    }

    override fun shouldPlay(hostVisible: Boolean, primarilyVisible: Boolean, wantsPlay: Boolean): Boolean =
        hostVisible && primarilyVisible && wantsPlay
}
