package com.arcovery.refreshratemanager.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalFloatingBottomBarTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.FloatingBottomBarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalFloatingBottomBarTabScale.current
    Column(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isInLightTheme = MiuixTheme.colorScheme.surfaceContainer.alpha < 0.5f
    val accentColor = MiuixTheme.colorScheme.primary
    val containerColor = if (isBlurEnabled) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }

    val backdrop = rememberLayerBackdrop()
    val density = LocalDensity.current

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val currentAnimValue = remember { Animatable(selectedIndex().toFloat()) }
    var isPressed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex()) {
        currentAnimValue.animateTo(
            selectedIndex().toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
        )
    }

    var currentIndex by remember(selectedIndex) { mutableIntStateOf(selectedIndex()) }

    LaunchedEffect(Unit) {
        snapshotFlow { selectedIndex() }.collectLatest {
            currentIndex = it
            onSelected(it)
        }
    }

    CompositionLocalProvider(
        LocalFloatingBottomBarTabScale provides {
            lerp(1f, 1.15f, isPressed)
        }
    ) {
        Box(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = contentWidthPx / tabsCount
                }
                .pointerInput(Unit) {
                    var dragAmountTotal = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragAmountTotal = 0f
                            isPressed = 1f
                        },
                        onDragEnd = {
                            isPressed = 0f
                            val localTabWidthPx = tabWidthPx
                            val localCurrentIndexFloat = currentAnimValue.value
                            val localCurrentIndex = currentIndex
                            if (localTabWidthPx > 0f) {
                                val dragInTabs = dragAmountTotal / localTabWidthPx
                                val targetIndex = (localCurrentIndexFloat + dragInTabs)
                                    .fastRoundToInt()
                                    .fastCoerceIn(0, tabsCount - 1)
                                if (targetIndex != localCurrentIndex) {
                                    currentIndex = targetIndex
                                    onSelected(targetIndex)
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            dragAmountTotal += dragAmount.x
                        }
                    )
                }
                .clip(CircleShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        if (isBlurEnabled) {
                            vibrancy()
                            blur(12f.dp.toPx())
                            lens(20f.dp.toPx(), 20f.dp.toPx())
                        }
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isBlurEnabled) 0.8f else 0f)
                    },
                    shadow = {
                        Shadow.Default.copy(
                            color = Color.Black.copy(if (isInLightTheme) 0.1f else 0.2f)
                        )
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .height(64.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = content
            )

            // Indicator pill
            if (tabWidthPx > 0f && totalWidthPx > 0f) {
                val contentWidth = totalWidthPx - with(density) { 8.dp.toPx() }
                val singleTabWidth = contentWidth / tabsCount
                val indicatorOffset = currentAnimValue.value * singleTabWidth

                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .graphicsLayer {
                            translationX = indicatorOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                if (isBlurEnabled) {
                                    lens(8f.dp.toPx(), 12f.dp.toPx())
                                }
                            },
                            shadow = { Shadow.Default.copy(alpha = 0.3f) },
                            onDrawSurface = {
                                drawRect(accentColor.copy(alpha = if (isInLightTheme) 0.15f else 0.25f))
                            }
                        )
                        .height(52.dp)
                        .width(with(density) { singleTabWidth.toDp() })
                )
            }
        }
    }
}

@Composable
fun FloatingBottomBarContainer(
    modifier: Modifier = Modifier,
    selectedIndex: () -> Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    isBlurEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding + 12.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FloatingBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            tabsCount = tabsCount,
            isBlurEnabled = isBlurEnabled,
            content = content
        )
    }
}
