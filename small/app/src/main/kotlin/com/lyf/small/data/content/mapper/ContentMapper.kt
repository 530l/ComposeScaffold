package com.lyf.small.data.content.mapper

import android.text.Html
import com.lyf.small.data.content.dto.WanArticleDto
import com.lyf.small.data.content.dto.WanBannerDto
import com.lyf.small.data.content.model.Article
import com.lyf.small.data.content.model.Banner

internal fun WanBannerDto.toModel(): Banner = Banner(
    id = id,
    title = title.decodeHtml(),
    imageUrl = imagePath,
)

internal fun WanArticleDto.toModel(): Article = Article(
    id = id,
    title = title.decodeHtml(),
    author = author.ifBlank { shareUser }.trim(),
    category = listOf(superChapterName, chapterName)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .joinToString(separator = " · "),
    date = niceDate,
    isFresh = fresh,
)

private fun String.decodeHtml(): String = Html.fromHtml(
    this,
    Html.FROM_HTML_MODE_LEGACY,
).toString().trim()
