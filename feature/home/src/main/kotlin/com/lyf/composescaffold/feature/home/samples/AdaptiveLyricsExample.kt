package com.lyf.composescaffold.feature.home.samples

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lyf.composescaffold.core.design.AppTheme

/**
 * 对应 README 第 5 节《嵌套作用域要先读取外层约束》。
 *
 * BoxWithConstraints 里再放 Column 时，两层布局作用域可能受到 DSL marker 的
 * 隐式接收者限制。需要外层约束时，先在进入内层作用域前算出普通值再使用：
 * 这样既明确了 maxHeight 来自外层，也避免了在内层直接访问时的
 * "cannot be called in this context with an implicit receiver" 编译错误。
 */
@Composable
private fun AdaptiveLyricsExample(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        // 先把外层约束读成普通值，再带进内层 Column 作用域。
        val showLyrics = maxHeight >= 160.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "歌曲名称",
                style = MaterialTheme.typography.titleMedium,
            )
            if (showLyrics) {
                Text(
                    text = "歌词预览",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** 高度充足（240dp ≥ 160dp）：展示歌词预览。 */
@Preview
@Composable
private fun AdaptiveLyricsTallPreview() {
    AppTheme {
        AdaptiveLyricsExample(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
    }
}

/** 高度不足（100dp < 160dp）：隐藏歌词预览。 */
@Preview
@Composable
private fun AdaptiveLyricsShortPreview() {
    AppTheme {
        AdaptiveLyricsExample(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        )
    }
}
