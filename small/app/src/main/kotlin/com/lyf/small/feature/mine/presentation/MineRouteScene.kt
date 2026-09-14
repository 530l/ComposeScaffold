package com.lyf.small.feature.mine.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lyf.small.R
import com.lyf.small.core.design.component.FeaturePlaceholder

@Composable
internal fun MineRouteScene() {
    FeaturePlaceholder(
        title = stringResource(R.string.feature_mine_title),
        description = stringResource(R.string.feature_mine_description),
    )
}
