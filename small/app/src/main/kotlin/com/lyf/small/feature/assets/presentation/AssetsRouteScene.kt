package com.lyf.small.feature.assets.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lyf.small.R
import com.lyf.small.core.design.component.FeaturePlaceholder

@Composable
internal fun AssetsRouteScene() {
    FeaturePlaceholder(
        title = stringResource(R.string.feature_assets_title),
        description = stringResource(R.string.feature_assets_description),
    )
}
