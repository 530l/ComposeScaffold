package com.lyf.composescaffold.feature.home.samples

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.lyf.composescaffold.core.design.AppTheme
import kotlinx.coroutines.delay

/**
 * 对应 README 第 3 节《长期运行的副作用需要最新回调》。
 *
 * 已启动的任务不应因外部回调变化而重新计时时，用 rememberUpdatedState：
 * 它把最新回调存进 State，LaunchedEffect 的 key 是 delayMs——延迟变化才重启任务；
 * 仅回调变化不重启任务，任务完成时调用的是最新回调。
 * 应按任务语义选择 effect 的 key，不要机械使用 LaunchedEffect(Unit)。
 */
@Composable
private fun DelayedNotice(
    delayMs: Long,
    onElapsed: () -> Unit,
) {
    val currentOnElapsed by rememberUpdatedState(onElapsed)

    LaunchedEffect(delayMs) {
        delay(delayMs)
        currentOnElapsed()
    }
}

/**
 * 演示：倒计时期间点击按钮会改变回调捕获的 clickCount，
 * 但任务不会重启；倒计时结束时读取的是最新的点击次数。
 */
@Composable
private fun DelayedNoticeExample() {
    var clickCount by rememberSaveable { mutableStateOf(0) }
    var notice by rememberSaveable { mutableStateOf("倒计时进行中，期间点击按钮不会重置计时…") }

    DelayedNotice(
        delayMs = 2_000L,
        onElapsed = { notice = "倒计时结束时你点了 $clickCount 次" },
    )

    Column {
        Text(notice)
        Button(onClick = { clickCount++ }) {
            Text("当前点击次数：$clickCount")
        }
    }
}

@Preview
@Composable
private fun DelayedNoticeExamplePreview() {
    AppTheme {
        DelayedNoticeExample()
    }
}
