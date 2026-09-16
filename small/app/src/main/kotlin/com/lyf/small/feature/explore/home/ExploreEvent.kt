package com.lyf.small.feature.explore.home

/** 一次性 UI 事件；刷新失败或登录态失效时显示非阻断提示。 */
internal enum class ExploreEvent {
    RefreshFailed,
    RefreshOffline,
    RequireLogin,
}
