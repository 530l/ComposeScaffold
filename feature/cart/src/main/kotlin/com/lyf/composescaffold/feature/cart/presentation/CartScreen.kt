package com.lyf.composescaffold.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.composescaffold.core.model.formatMoney
import com.lyf.composescaffold.core.design.AppTheme
import com.lyf.composescaffold.core.design.ui.event.ObserveAsEvents
import com.lyf.composescaffold.core.design.ui.loadmore.LoadableLazyColumn
import com.lyf.composescaffold.core.design.ui.loadmore.LoadMoreState
import com.lyf.composescaffold.core.design.ui.state.LoadableErrorBanner
import com.lyf.composescaffold.core.design.ui.state.LoadableStateContent
import com.lyf.composescaffold.feature.cart.R
import com.lyf.composescaffold.feature.cart.domain.Article

@Composable
internal fun CartScreen(
    onCheckout: (selectedItemIds: List<Long>) -> Unit = {},
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CartEvent.Checkout -> onCheckout(event.selectedItemIds)
        }
    }
    CartContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun CartContent(
    uiState: CartUiState,
    onIntent: (CartIntent) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SettleBar(
                uiState = uiState,
                onIntent = onIntent,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.cart_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
            )

            LoadableStateContent(
                isInitializing = uiState.isInitializing,
                isEmpty = uiState.dataList.isEmpty(),
                errorMessage = uiState.error?.let { error -> errorMessage(error) },
                loadingText = stringResource(R.string.cart_loading),
                emptyText = stringResource(R.string.cart_empty),
                retryText = stringResource(R.string.cart_retry),
                onRetry = { onIntent(CartIntent.Retry) },
            ) {
                ArticleList(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }
        }
    }
}

private fun LazyListScope.articleListContent(
    uiState: CartUiState,
    onIntent: (CartIntent) -> Unit,
) {
    uiState.error
        ?.takeUnless { uiState.loadMoreState == LoadMoreState.Failed }
        ?.let { error ->
            item(key = "cart_error") {
                LoadableErrorBanner(message = errorMessage(error))
            }
        }
    items(uiState.dataList, key = { item -> item.article.id }) { item ->
        ArticleRow(
            item = item,
            onToggle = { onIntent(CartIntent.ToggleItem(item.article.id)) },
        )
    }
}

@Composable
private fun ArticleList(
    uiState: CartUiState,
    onIntent: (CartIntent) -> Unit,
) {
    LoadableLazyColumn(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = uiState.isRefreshing,
        loadMoreState = uiState.loadMoreState,
        onRefresh = { onIntent(CartIntent.Refresh) },
        onLoadMore = { onIntent(CartIntent.LoadMore) },
        content = { articleListContent(uiState, onIntent) },
    )
}

@Composable
private fun ArticleRow(
    item: CartItemUiState,
    onToggle: () -> Unit,
) {
    val article = item.article
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .toggleable(
                value = item.selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArticleThumbnail(article)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.cart_article_subtitle,
                    formatMoney(item.unitPrice),
                    article.chapterName,
                    article.niceDate,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = item.selected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ArticleThumbnail(article: Article) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(thumbnailColor(article.id)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = article.chapterName.firstOrNull()?.toString() ?: "#",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettleBar(
    uiState: CartUiState,
    onIntent: (CartIntent) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val allState = when {
                uiState.allSelected -> ToggleableState.On
                uiState.selectedCount > 0 -> ToggleableState.Indeterminate
                else -> ToggleableState.Off
            }
            Row(
                modifier = Modifier.triStateToggleable(
                    state = allState,
                    enabled = uiState.dataList.isNotEmpty(),
                    role = Role.Checkbox,
                    onClick = { onIntent(CartIntent.ToggleSelectAll) },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriStateCheckbox(
                    state = allState,
                    onClick = null,
                    enabled = uiState.dataList.isNotEmpty(),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text(
                    text = stringResource(R.string.cart_select_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.cart_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(uiState.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { onIntent(CartIntent.Checkout) },
                enabled = uiState.selectedCount > 0,
                modifier = Modifier.height(44.dp),
            ) {
                Text(stringResource(R.string.cart_checkout, uiState.selectedCount))
            }
        }
    }
}

/** 演示用缩略图调色板：接入真实设计系统时应替换为 theme 扩展并适配深色模式。*/
private val thumbnailColors = listOf(
    Color(0xFFF7E8D3),
    Color(0xFFE2EFFA),
    Color(0xFFEFE7FB),
    Color(0xFFE6F4E4),
    Color(0xFFFCEBDC),
    Color(0xFFE3F1EF),
)

private fun thumbnailColor(articleId: Long): Color =
    thumbnailColors[articleId.mod(thumbnailColors.size)]

@Composable
private fun errorMessage(error: CartError): String = when (error) {
    CartError.LOAD_FAILED -> stringResource(R.string.cart_error_load)
}

@Preview
@Composable
private fun CartContentPreview() {
    AppTheme {
        CartContent(
            uiState = CartUiState(
                isInitializing = false,
                dataList = listOf(
                    CartItemUiState(
                        article = Article(1, "Kotlin 与 Java，不是简单的高低之分", "化骨龙", "广场Tab", "https://example.com/1", "1天前"),
                        unitPrice = demoUnitPrice(0),
                    ),
                    CartItemUiState(
                        article = Article(2, "Compose Multiplatform 1.11 发布", "官方", "资讯", "https://example.com/2", "2天前"),
                        unitPrice = demoUnitPrice(1),
                        selected = true,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
