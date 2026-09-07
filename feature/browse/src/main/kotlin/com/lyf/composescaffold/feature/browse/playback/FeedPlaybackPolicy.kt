package com.lyf.composescaffold.feature.browse.playback

/** Feed 信息流播放决策策略契约。 */
internal interface FeedPlaybackPolicy {
    /** 评估播放器池容量 */
    fun capacity(lowRam: Boolean): Int
    /** 预判下一项待预加载的条目下标 */
    fun neighbor(settled: Int, current: Int, target: Int, itemCount: Int): Int?
    /** 判定是否允许起播 */
    fun shouldPlay(hostVisible: Boolean, primarilyVisible: Boolean, wantsPlay: Boolean): Boolean
}

/** 竖屏短视频信息流播放策略默认实现。 */
internal class ShortVideoPlaybackPolicy : FeedPlaybackPolicy {
    /** 低内存设备仅保留当前 1 台播放器；正常设备保留 2 台支持顺滑预热 */
    override fun capacity(lowRam: Boolean): Int = if (lowRam) 1 else 2

    /** 预测下一个邻近需要预加载的项目下标。 */
    override fun neighbor(settled: Int, current: Int, target: Int, itemCount: Int): Int? {
        val index = when {
            target != settled -> target
            current < settled -> settled - 1
            else -> settled + 1
        }
        return index.takeIf { it in 0 until itemCount }
    }

    /** 门控判定：当且仅当宿主可见、当前处于主视图、且用户意图为播放时才允许起播。 */
    override fun shouldPlay(hostVisible: Boolean, primarilyVisible: Boolean, wantsPlay: Boolean): Boolean =
        hostVisible && primarilyVisible && wantsPlay
}
