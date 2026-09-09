package com.lyf.composescaffold.feature.browse.presentation.mv.playback

import com.lyf.composescaffold.core.model.feed.FeedItem
import com.lyf.composescaffold.core.model.feed.FeedMedia
import com.lyf.composescaffold.core.model.feed.FeedOrientation
import com.lyf.composescaffold.core.player.media.MediaKind
import com.lyf.composescaffold.core.player.media.MediaSource

/** 业务模型只在入口映射，通用播放器不认识 Feed。 */
internal fun FeedItem.toMediaSource(): MediaSource = MediaSource(
    id = key,
    uri = media.url,
    kind = if (media is FeedMedia.Mv) MediaKind.VIDEO else MediaKind.AUDIO,
    title = title,
    artist = artist,
    artworkUri = coverUrl,
    // 实际尺寸未知时先按方向预留比例，后续由视频尺寸校正。
    initialAspectRatio = if ((media as? FeedMedia.Mv)?.orientation == FeedOrientation.LANDSCAPE) 16f / 9f else 9f / 16f,
)
