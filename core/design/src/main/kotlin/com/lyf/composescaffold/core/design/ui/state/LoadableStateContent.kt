package com.lyf.composescaffold.core.design.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 可加载页面的通用整页状态容器。
 *
 * 这里只统一状态切换和视觉骨架；业务文案、重试行为与正常内容由 feature 传入。
 * 已有内容时的非阻断错误不在此处理，可使用 [LoadableErrorBanner] 展示。
 */
@Composable
fun LoadableStateContent(
    isInitializing: Boolean,
    isEmpty: Boolean,
    errorMessage: String?,
    loadingText: String,
    emptyText: String,
    retryText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isInitializing -> LoadingState(text = loadingText)
            errorMessage != null && isEmpty -> ErrorState(
                message = errorMessage,
                retryText = retryText,
                onRetry = onRetry,
            )

            isEmpty -> EmptyState(text = emptyText)
            else -> content()
        }
    }
}

/** 已有内容时使用的非阻断错误提示。 */
@Composable
fun LoadableErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoadingState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    retryText: String,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) {
                Text(retryText)
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
