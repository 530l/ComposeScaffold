package com.lyf.small.feature.creation.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lyf.small.R
import com.lyf.small.core.design.component.FeaturePlaceholder

@Composable
internal fun CreationRouteScene() {
    FeaturePlaceholder(
        title = stringResource(R.string.feature_creation_title),
        description = stringResource(R.string.feature_creation_description),
    )
}
