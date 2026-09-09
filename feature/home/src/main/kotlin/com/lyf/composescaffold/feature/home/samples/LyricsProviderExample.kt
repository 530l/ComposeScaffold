package com.lyf.composescaffold.feature.home.samples

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.lyf.composescaffold.core.design.AppTheme

/**
 * 对应 README 第 3 节《通过函数获取状态：() -> T》。
 *
 * 传状态值是"给你当前值"，传 provider 是"给你一个读取入口"。
 * 注意边界：() -> T 本身不是可观察状态，不会自动监听变化——
 * 本例能响应变化，是因为在组合期间调用 expandedProvider() 时读取了 Compose State；
 * 如果 provider 只返回普通可变变量，变化不会自动触发重组。
 * 普通展示参数仍优先传 expanded: Boolean；由调用方决定读取时机时才用 () -> T。
 */
@Composable
private fun LyricsPanelWithProvider(
    expandedProvider: () -> Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    // 在组合期间调用 provider：读到的 State 会被快照系统跟踪，状态变化时本组件重组。
    val expanded = expandedProvider()

    TextButton(onClick = { onExpandedChange(!expanded) }) {
        Text(if (expanded) "收起歌词" else "展开歌词")
    }
}

@Composable
private fun ProviderExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LyricsPanelWithProvider(
        expandedProvider = { expanded },
        onExpandedChange = { expanded = it },
    )
}

@Preview
@Composable
private fun ProviderExamplePreview() {
    AppTheme {
        ProviderExample()
    }
}
