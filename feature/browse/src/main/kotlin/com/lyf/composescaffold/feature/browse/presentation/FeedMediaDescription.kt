package com.lyf.composescaffold.feature.browse.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedOrientation
import com.lyf.composescaffold.feature.browse.R

/** 只从媒体已有字段生成说明，不推断时长、热度或发布时间。 */
@Composable
internal fun feedMediaDescription(media: FeedMedia): String = when (media) {
    is FeedMedia.Music -> when {
        media.instrumental -> stringResource(R.string.feed_type_instrumental)
        media.lyrics.isNotEmpty() -> stringResource(R.string.feed_type_lyrics, media.lyrics.size)
        else -> stringResource(R.string.feed_music)
    }
    is FeedMedia.Mv -> stringResource(
        when (media.orientation) {
            FeedOrientation.LANDSCAPE -> R.string.feed_type_landscape
            FeedOrientation.PORTRAIT -> R.string.feed_type_portrait
            FeedOrientation.UNKNOWN -> R.string.feed_mv
        },
    )
}
