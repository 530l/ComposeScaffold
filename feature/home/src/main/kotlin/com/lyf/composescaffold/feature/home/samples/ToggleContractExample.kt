package com.lyf.composescaffold.feature.home.samples

import androidx.compose.foundation.layout.Column
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
 * 对应 README 第 4 节《事件回调：通知操作，不一定传出新状态》。
 *
 * 两种契约都常用，取决于组件想表达什么：
 * - onExpandedChange: (Boolean) -> Unit：请求设置为指定状态，父级直接得到目标值；
 * - onToggle: () -> Unit：只通知"发生了切换"，新状态由外部根据自己持有的最新状态计算。
 */
@Composable
private fun ExpandButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    TextButton(onClick = { onExpandedChange(!expanded) }) {
        Text(if (expanded) "收起详情" else "展开详情")
    }
}

@Composable
private fun ToggleButton(
    onToggle: () -> Unit,
) {
    // 组件的 onClick 一般保持 () -> Unit；异步业务操作通过 Intent 交给 ViewModel，
    // 不要在组合函数体中直接发起业务请求。
    TextButton(onClick = onToggle) {
        Text("静音切换")
    }
}

@Composable
private fun ToggleContractExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var muted by rememberSaveable { mutableStateOf(false) }

    Column {
        // 契约一：外部直接得到目标值。
        ExpandButton(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        )
        // 契约二：外部基于自己持有的 muted 计算新值。
        ToggleButton(onToggle = { muted = !muted })

        if (expanded) {
            Text("详情内容")
        }
        Text(if (muted) "当前：静音" else "当前：外放")
    }
}

@Preview
@Composable
private fun ToggleContractExamplePreview() {
    AppTheme {
        ToggleContractExample()
    }
}
