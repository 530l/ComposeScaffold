package com.lyf.small.feature.explore.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyf.small.R
import com.lyf.small.data.content.model.Article
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 单篇文章卡片:作者行(含「新」徽标)、标题、分类与日期。 */
@Composable
internal fun ExploreArticleCard(article: Article) {
    val author = article.author.ifBlank {
        stringResource(R.string.feature_explore_anonymous_author)
    }
    val category = article.category.ifBlank {
        stringResource(R.string.feature_explore_uncategorized)
    }
    Card(
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = author,
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote1,
            )
            if (article.isFresh) {
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.primaryContainer,
                        contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                    cornerRadius = BADGE_CORNER_RADIUS,
                    insideMargin = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feature_explore_fresh),
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = article.title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.headline1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category,
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.footnote1,
            )
            if (article.date.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = article.date,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
}

/** 「新」徽标的小圆角。 */
private val BADGE_CORNER_RADIUS = 8.dp
