package com.lyf.small.feature.explore.home

/** 一次性 UI 事件；刷新任一数据源失败时显示非阻断提示。 */
internal enum class ExploreEvent {
    RefreshFailed,
    RefreshOffline,
}
