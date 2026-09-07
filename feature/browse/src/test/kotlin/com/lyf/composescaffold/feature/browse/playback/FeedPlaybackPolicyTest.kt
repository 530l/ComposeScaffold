package com.lyf.composescaffold.feature.browse.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedPlaybackPolicyTest {
    private val policy = ShortVideoPlaybackPolicy()

    @Test
    fun capacity_adaptsToLowRamDevice() {
        assertThat(policy.capacity(lowRam = true)).isEqualTo(1)
        assertThat(policy.capacity(lowRam = false)).isEqualTo(2)
    }

    @Test
    fun neighbor_prefetchesTargetWhenMoving() {
        // 当明确有目标页面时，优先预热 target
        val next = policy.neighbor(settled = 0, current = 0, target = 1, itemCount = 5)
        assertThat(next).isEqualTo(1)
    }

    @Test
    fun neighbor_prefetchesDownwardWhenScrollingForward() {
        // 当前页大于已停靠页（向下滑动），预热下一个
        val next = policy.neighbor(settled = 1, current = 2, target = 1, itemCount = 5)
        assertThat(next).isEqualTo(2)
    }

    @Test
    fun neighbor_prefetchesUpwardWhenScrollingBackward() {
        // 当前页小于已停靠页（向上滑动），预热上一个
        val next = policy.neighbor(settled = 2, current = 1, target = 2, itemCount = 5)
        assertThat(next).isEqualTo(1)
    }

    @Test
    fun neighbor_returnsNullWhenOutOfBounds() {
        // 顶部反向越界
        val top = policy.neighbor(settled = 0, current = -1, target = 0, itemCount = 5)
        assertThat(top).isNull()

        // 底部正向越界
        val bottom = policy.neighbor(settled = 4, current = 4, target = 4, itemCount = 5)
        assertThat(bottom).isNull()
    }

    @Test
    fun shouldPlay_requiresAllConditions() {
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = true, wantsPlay = true)).isTrue()
        assertThat(policy.shouldPlay(hostVisible = false, primarilyVisible = true, wantsPlay = true)).isFalse()
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = false, wantsPlay = true)).isFalse()
        assertThat(policy.shouldPlay(hostVisible = true, primarilyVisible = true, wantsPlay = false)).isFalse()
    }
}
