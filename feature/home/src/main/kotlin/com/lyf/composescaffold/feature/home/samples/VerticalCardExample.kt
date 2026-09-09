package com.lyf.composescaffold.feature.home.samples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lyf.composescaffold.core.design.AppTheme

/**
 * 对应 README 第 5 节《带接收者的插槽》。
 *
 * content: @Composable ColumnScope.() -> Unit 表示调用插槽时提供 ColumnScope 接收者，
 * 插槽内可以使用该布局作用域的能力（如 Modifier.weight、Modifier.align）。
 * 它描述的是布局能力，与状态是否提升是两回事。
 */
@Composable
private fun VerticalCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(content = content)
}

@Composable
private fun VerticalCardExample() {
    var collected by rememberSaveable { mutableStateOf(false) }

    VerticalCard {
        Text(
            text = "歌曲名称",
            style = MaterialTheme.typography.titleMedium,
        )
        // align 来自 ColumnScope 接收者：不在 Column 直接子组件位置就拿不到这个能力。
        Text(
            text = "演唱者",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.End),
        )
        TextButton(onClick = { collected = !collected }) {
            Text(if (collected) "已收藏" else "收藏")
        }
    }
}

@Preview
@Composable
private fun VerticalCardExamplePreview() {
    AppTheme {
        VerticalCardExample()
    }
}
