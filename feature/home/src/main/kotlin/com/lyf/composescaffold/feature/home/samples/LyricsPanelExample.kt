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
 * 对应 README 第 2 节《状态提升：状态值 + 修改回调》。
 *
 * LyricsPanel 是无状态展示组件：它不持有"是否展开"，
 * 只负责两件事——按传入的 expanded 显示 UI，点击时通过回调请求修改。
 * 不要在子组件里用 remember { mutableStateOf(expanded) } 复制一份相同状态：
 * 那样会出现两份状态，外部后续变化也不会同步到这份初始化副本。
 */
@Composable
private fun LyricsPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    // 点击只是"请求"修改：父级才是真正的状态持有者，可以接受、校验或拒绝该请求。
    TextButton(onClick = { onExpandedChange(!expanded) }) {
        Text(if (expanded) "收起歌词" else "展开歌词")
    }

    if (expanded) {
        Text("这里显示歌词内容")
    }
}

/**
 * 父级持有状态并回写，数据流是：
 * 父级状态 expanded → 子组件显示 → 点击 → onExpandedChange(新值) → 父级决定是否更新 → 子组件接收新状态。
 * rememberSaveable 让展开状态在 Activity 重建（如旋转屏幕）后仍可恢复。
 */
@Composable
private fun LyricsPanelExample() {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LyricsPanel(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    )
}

@Preview
@Composable
private fun LyricsPanelExamplePreview() {
    AppTheme {
        // LyricsPanel 会输出两个兄弟节点，包一层 Column 让它们在预览中纵向排列。
        Column {
            LyricsPanelExample()
        }
    }
}
