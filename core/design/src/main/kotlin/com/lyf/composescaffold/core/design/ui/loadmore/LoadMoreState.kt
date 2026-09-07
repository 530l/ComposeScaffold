package com.lyf.composescaffold.core.design.ui.loadmore

/**
 * 加载更多的 UI 状态。刷新/初始化的失败不进这里，
 * 由 [LoadableController] 的错误回调交给 feature 自行分层处理。
 */
sealed interface LoadMoreState {
    /** 空闲：可以触发下一次加载，footer 不显示内容。 */
    data object Idle : LoadMoreState

    /** 加载中：footer 显示进度，状态机会拒绝重复触发。 */
    data object Loading : LoadMoreState

    /** 加载失败：footer 显示重试入口，不自动重试，等用户点击或刷新。 */
    data object Failed : LoadMoreState

    /** 没有更多数据：footer 是否显示结束文案由容器参数控制。 */
    data object End : LoadMoreState
}

/**
 * 单页加载结果。
 *
 * [hasMore] 必须由调用方根据后端分页信号（cursor 是否为空、总数等）显式给出，
 * 不要用「返回条数 < pageSize」推断，后端不满页返回时会误判。
 */
data class Page<T>(
    val items: List<T>,
    val hasMore: Boolean,
    /** 服务端原始条数；调用方去重或过滤时仍须保留，避免把无新增项误判为末页。 */
    val sourceItemCount: Int = items.size,
) {
    init {
        require(sourceItemCount >= items.size) { "原始条数不能少于展示条数" }
    }
}
