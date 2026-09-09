package com.lyf.composescaffold.feature.browse.presentation.mv.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 短视频播放策略的池容量、邻接预热与起播条件测试。 */
class FeedPlaybackPolicyTest {
    private val policy = ShortVideoPlaybackPolicy()

    /** 低内存设备池容量降为一，普通设备为二。 */
    @Test
    fun capacity_adaptsToLowRamDevice() {
        assertThat(policy.capacity(lowRam = true)).isEqualTo(1)
        assertThat(policy.capacity(lowRam = false)).isEqualTo(2)
    }

    /** 有明确目标页面时优先预热目标本身。 */
    @Test
    fun neighbor_prefetchesTargetWhenMoving() {
        val next = policy.neighbor(settled = 0, current = 0, target = 1, itemCount = 5)
        assertThat(next).isEqualTo(1)
    }

    /** 当前页大于已停靠页（向下滑动）时预热下一项。 */
    @Test
    fun neighbor_prefetchesDownwardWhenScrollingForward() {
        val next = policy.neighbor(settled = 1, current = 2, target = 1, itemCount = 5)
        assertThat(next).isEqualTo(2)
    }

    /** 当前页小于已停靠页（向上滑动）时预热上一项。 */
    @Test
    fun neighbor_prefetchesUpwardWhenScrollingBackward() {
        val next = policy.neighbor(settled = 2, current = 1, target = 2, itemCount = 5)
        assertThat(next).isEqualTo(1)
    }

    /** 越界方向没有可预热项时返回空。 */
    @Test
    fun neighbor_returnsNullWhenOutOfBounds() {
        val top = policy.neighbor(settled = 0, current = -1, target = 0, itemCount = 5)
        assertThat(top).isNull()

        val bottom = policy.neighbor(settled = 4, current = 4, target = 4, itemCount = 5)
        assertThat(bottom).isNull()
    }

    /** 起播要求宿主可见、主可见且想播三个条件同时成立。 */
    @Test
    fun shouldPlay_requiresAllConditions() {
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = true, wantsPlay = true)).isTrue()
        assertThat(policy.shouldPlay(hostVisible = false, primarilyVisible = true, wantsPlay = true)).isFalse()
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = false, wantsPlay = true)).isFalse()
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = true, wantsPlay = false)).isFalse()
    }
}
