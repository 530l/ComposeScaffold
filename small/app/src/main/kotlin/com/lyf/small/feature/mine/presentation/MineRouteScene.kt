package com.lyf.small.feature.mine.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lyf.small.R
import com.lyf.small.core.design.component.FullPageStateCard

/** 未登录占位态：整页卡片提示 + 登录入口。 */
@Composable
internal fun MineRouteScene(
    onNavigateToLogin: () -> Unit,
) {
    FullPageStateCard(
        title = stringResource(R.string.feature_mine_not_logged_in),
        description = stringResource(R.string.feature_mine_login_description),
        action = stringResource(R.string.feature_mine_login_entry),
        onAction = onNavigateToLogin,
    )
}
