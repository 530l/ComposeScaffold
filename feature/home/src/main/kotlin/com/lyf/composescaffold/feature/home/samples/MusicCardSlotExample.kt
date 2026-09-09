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
 * 对应 README 第 5 节《UI 插槽：外部决定内容长什么样》。
 *
 * MusicCard 只约定"标题 + 一段外部提供的 UI"：
 * actions 是 @Composable () -> Unit 插槽，必须从组合上下文调用；
 * 普通 () -> Unit 只能当事件回调，不能在组合里当 UI 调用，两者不能混用。
 */
@Composable
private fun MusicCard(
    title: String,
    actions: @Composable () -> Unit,
) {
    Column {
        Text(title)
        actions()
    }
}

/**
 * 最后一个参数是函数时可以用尾随 lambda：这里相当于 actions = { ... }。
 * 首页里的真实例子：Scaffold(topBar = { ... }) 把顶部栏交给外部定义，
 * CenterAlignedTopAppBar(title = { ... }) 把标题 UI 交给外部定义。
 */
@Composable
private fun SlotExample(onLyrics: () -> Unit) {
    MusicCard(title = "歌曲名称") {
        TextButton(onClick = onLyrics) {
            Text("查看歌词")
        }
    }
}

@Preview
@Composable
private fun SlotExamplePreview() {
    AppTheme {
        // 预览里给回调接一份真实状态，点击后能看到插槽与事件回调如何配合。
        var showLyrics by rememberSaveable { mutableStateOf(false) }

        Column {
            SlotExample(onLyrics = { showLyrics = !showLyrics })

            if (showLyrics) {
                Text("歌词内容")
            }
        }
    }
}
