package com.lyf.composescaffold.core.ui.loadmore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.R

/**
 * 加载更多 footer。各状态固定相同高度，避免状态切换时列表底部跳动；
 * Failed 提供重试入口，End 是否显示「没有更多」由 [showEndText] 控制。
 * Failed 的按钮与说明文字统一 bodySmall 字号，状态切换不产生行高跳变。
 */
@Composable
fun LoadMoreFooter(
    state: LoadMoreState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    showEndText: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LOAD_MORE_FOOTER_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            LoadMoreState.Idle -> Unit
            LoadMoreState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )

            LoadMoreState.Failed -> Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.core_load_more_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.core_retry),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            LoadMoreState.End -> if (showEndText) {
                EndDividerText()
            }
        }
    }
}

/** 「— 没有更多了 —」收尾：两侧发丝线托住文案，比孤行文字更安定。 */
@Composable
private fun EndDividerText() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = Dp.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = stringResource(R.string.core_no_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = Dp.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

private val LOAD_MORE_FOOTER_HEIGHT = 56.dp
