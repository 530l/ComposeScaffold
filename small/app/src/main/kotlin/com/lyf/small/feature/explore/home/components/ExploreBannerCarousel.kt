package com.lyf.small.feature.explore.home.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyf.small.R
import com.lyf.small.core.design.image.AppImage
import com.lyf.small.data.content.model.Banner
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ExploreBannerCarousel(
    banners: List<Banner>,
    modifier: Modifier = Modifier,
) {
    // 调用方约定只在非空时组合本组件;组件自身再防一道,空列表不再依赖调用方不变量。
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })
    AutoAdvanceBanner(
        pagerState = pagerState,
        pageCount = banners.size,
    )

    Card(
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BANNER_ASPECT_RATIO),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> banners[page].id },
            ) { page ->
                val banner = banners[page]
                AppImage(
                    imageUrl = banner.imageUrl,
                    contentDescription = stringResource(
                        R.string.feature_explore_banner_image_description,
                        banner.title,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            BannerCaption(
                banner = banners[pagerState.currentPage.coerceIn(banners.indices)],
                pagerState = pagerState,
                pageCount = banners.size,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun BannerCaption(
    banner: Banner,
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                // 透明到黑的垂直渐变替代纯色横条,标题区与图片自然融合。
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = banner.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MiuixTheme.textStyles.body1,
        )
        if (pageCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                DotsIndicator(
                    dotCount = pageCount,
                    dotSpacing = 6.dp,
                    type = ShiftIndicatorType(
                        dotsGraphic = DotGraphic(
                            size = 6.dp,
                            color = MiuixTheme.colorScheme.primary,
                        ),
                    ),
                    pagerState = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AutoAdvanceBanner(
    pagerState: PagerState,
    pageCount: Int,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isUserDragging by pagerState.interactionSource.collectIsDraggedAsState()
    // Compose 的 LocalAccessibilityManager 不暴露触摸探索状态，改读系统服务。
    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    // 响应式订阅:会话中途开关触摸探索(TalkBack)也能立即暂停/恢复自动翻页。
    var touchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }
    DisposableEffect(accessibilityManager) {
        val manager = accessibilityManager
        if (manager == null) {
            onDispose { }
        } else {
            val listener = AccessibilityManager.TouchExplorationStateChangeListener {
                touchExplorationEnabled = manager.isTouchExplorationEnabled
            }
            manager.addTouchExplorationStateChangeListener(listener)
            onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
        }
    }

    LaunchedEffect(
        pagerState,
        pageCount,
        lifecycleState,
        touchExplorationEnabled,
        isUserDragging,
    ) {
        // 自动轮播仅在多页、处于前台且用户未接管（触摸探索/拖动）时进行。
        val notInteractive = !lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
        val userControlled = touchExplorationEnabled || isUserDragging
        if (pageCount <= 1 || notInteractive || userControlled) {
            return@LaunchedEffect
        }
        // 用户按下拖动时 effect 会立即取消；自动动画不会改变 isUserDragging。
        snapshotFlow { pagerState.settledPage }.collectLatest {
            delay(BANNER_AUTO_ADVANCE_MILLIS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pageCount)
            }
        }
    }
}

private const val BANNER_ASPECT_RATIO = 16f / 9f
private const val BANNER_AUTO_ADVANCE_MILLIS = 5_000L
