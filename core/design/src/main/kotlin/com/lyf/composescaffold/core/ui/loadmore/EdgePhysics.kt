package com.lyf.composescaffold.core.ui.loadmore

/**
 * 底部橡胶带阻尼数学。纯函数、无 Compose 依赖，便于单测。
 *
 * 方向约定与 nested scroll 一致：delta/velocity 的 y < 0 表示向列表末端滚动，
 * 回弹位移恒为负值（内容向末端方向越过边界），0 表示无回弹。
 */
internal object EdgePhysics {

    data class Result(
        val offsetPx: Float,
        val consumedDeltaPx: Float,
    )

    /**
     * 拖拽越界：按 [damping] 阻尼累积位移，并夹在 [−maxOffsetPx, 0]。
     * 返回消耗掉的滚动 delta（未夹紧时等于全部输入，夹紧后为部分）。
     */
    fun drag(offsetPx: Float, deltaPx: Float, maxOffsetPx: Float, damping: Float): Result {
        val newOffset = (offsetPx + deltaPx * damping).coerceIn(-maxOffsetPx, 0f)
        val consumed = if (damping > 0f) (newOffset - offsetPx) / damping else 0f
        return Result(offsetPx = newOffset, consumedDeltaPx = consumed)
    }

    /** 反向滚动时 1:1 收拢已有回弹，收拢量即消耗量。 */
    fun collapse(offsetPx: Float, deltaPx: Float): Result {
        val consumed = deltaPx.coerceAtMost(-offsetPx)
        return Result(offsetPx = offsetPx + consumed, consumedDeltaPx = consumed)
    }

    /** fling 剩余速度折算成一次越界冲量，再走夹紧逻辑。 */
    fun kick(offsetPx: Float, velocityPx: Float, maxOffsetPx: Float, factor: Float): Float =
        (offsetPx + velocityPx * factor).coerceIn(-maxOffsetPx, 0f)
}
