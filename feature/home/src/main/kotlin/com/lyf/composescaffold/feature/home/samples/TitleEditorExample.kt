package com.lyf.composescaffold.feature.home.samples

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.lyf.composescaffold.core.design.AppTheme

/**
 * 对应 README 第 2 节《输入框是同一种模式》。
 *
 * 输入框和开关是同一种契约：value: T + onValueChange: (T) -> Unit。
 * TitleEditor 没有额外逻辑时直接转交回调：onValueChange = onTitleChange；
 * 需要加工时才包装一层：onValueChange = { newTitle -> onTitleChange(newTitle) }。
 */
@Composable
private fun TitleEditor(
    title: String,
    onTitleChange: (String) -> Unit,
) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
    )
}

/** 状态同样提升到父级，子组件里没有任何一份复制的状态。 */
@Composable
private fun TitleEditorExample() {
    var title by rememberSaveable { mutableStateOf("未命名歌单") }

    TitleEditor(
        title = title,
        onTitleChange = { title = it },
    )
}

@Preview
@Composable
private fun TitleEditorExamplePreview() {
    AppTheme {
        TitleEditorExample()
    }
}
