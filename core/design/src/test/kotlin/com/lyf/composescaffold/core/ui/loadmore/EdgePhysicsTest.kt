package com.lyf.composescaffold.core.ui.loadmore

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EdgePhysicsTest {

    @Test
    fun dragAccumulatesWithDamping() {
        val result = EdgePhysics.drag(offsetPx = -10f, deltaPx = -20f, maxOffsetPx = 100f, damping = 0.5f)
        assertThat(result.offsetPx).isEqualTo(-20f)
        // 全量消耗：位移增量 (-20 - (-10)) 折回阻尼前的 delta。
        assertThat(result.consumedDeltaPx).isEqualTo(-20f)
    }

    @Test
    fun dragClampsAtMaxOffsetAndConsumesPartially() {
        val result = EdgePhysics.drag(offsetPx = -95f, deltaPx = -20f, maxOffsetPx = 100f, damping = 0.5f)
        assertThat(result.offsetPx).isEqualTo(-100f)
        // 夹紧后只消耗了 5px 位移对应的 delta。
        assertThat(result.consumedDeltaPx).isEqualTo(-10f)
    }

    @Test
    fun dragNeverProducesPositiveOffset() {
        val result = EdgePhysics.drag(offsetPx = -30f, deltaPx = 40f, maxOffsetPx = 100f, damping = 0.5f)
        // 反向 delta 不属于 drag 的职责（由 collapse 处理），但也不允许越界为正。
        assertThat(result.offsetPx).isAtMost(0f)
    }

    @Test
    fun collapseCollapsesOneToOne() {
        val result = EdgePhysics.collapse(offsetPx = -30f, deltaPx = 10f)
        assertThat(result.offsetPx).isEqualTo(-20f)
        assertThat(result.consumedDeltaPx).isEqualTo(10f)
    }

    @Test
    fun collapseStopsAtZero() {
        val result = EdgePhysics.collapse(offsetPx = -30f, deltaPx = 50f)
        assertThat(result.offsetPx).isEqualTo(0f)
        assertThat(result.consumedDeltaPx).isEqualTo(30f)
    }

    @Test
    fun kickConvertsVelocityClamped() {
        val clamped = EdgePhysics.kick(offsetPx = 0f, velocityPx = -10_000f, maxOffsetPx = 100f, factor = 0.05f)
        assertThat(clamped).isEqualTo(-100f)
        val normal = EdgePhysics.kick(offsetPx = 0f, velocityPx = -1_000f, maxOffsetPx = 100f, factor = 0.05f)
        assertThat(normal).isEqualTo(-50f)
        // 正向速度不产生末端回弹。
        assertThat(EdgePhysics.kick(offsetPx = 0f, velocityPx = 1_000f, maxOffsetPx = 100f, factor = 0.05f))
            .isEqualTo(0f)
    }
}
