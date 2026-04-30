package com.arcovery.refreshratemanager.ui.miuix

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import com.arcovery.refreshratemanager.utils.RefreshRateManager

@Composable
fun MiuixConfigScreen(
    refreshRateManager: RefreshRateManager,
    useRoot: Boolean,
    useShizuku: Boolean,
    paddingValues: PaddingValues
) {
    val supportedRates = remember { refreshRateManager.getSupportedRefreshRates() }
    var activeHz by remember { mutableIntStateOf(-1) }
    var showNoPermDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // 实时 FPS 监控
    val realtimeFps by refreshRateManager.getRealtimeFps().collectAsState(initial = 0f)

    // 启动时获取当前选中的刷新率档位
    LaunchedEffect(Unit) {
        activeHz = refreshRateManager.getCurrentRefreshRate().toInt()
    }

    // 刷新支持的档位
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            activeHz = refreshRateManager.getCurrentRefreshRate().toInt()
            isRefreshing = false
        }
    }

    // 无权限提醒弹窗
    OverlayDialog(
        show = showNoPermDialog,
        title = "需要权限",
        onDismissRequest = { showNoPermDialog = false }
    ) {
        Text("切换刷新率需要 Root 或 Shizuku 权限，请在设置页开启对应模式。")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = "知道了",
                onClick = { showNoPermDialog = false }
            )
        }
    }

    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        pullToRefreshState = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 顶部卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "运行模式",
                                fontSize = 15.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            val modeText = when {
                                useRoot -> "Root"
                                useShizuku -> "Shizuku"
                                else -> "未启用"
                            }
                            val modeColor = when {
                                useRoot -> Color(0xFF34C759)
                                useShizuku -> Color(0xFF3478F6)
                                else -> MiuixTheme.colorScheme.onSurfaceVariantActions
                            }
                            Text(
                                text = modeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = modeColor
                            )
                        }
                        if (realtimeFps > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "当前刷新率",
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${realtimeFps.toInt()} Hz",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 刷新率档位选择卡片
            itemsIndexed(supportedRates) { _, hz ->
                MiuixRefreshRateCard(
                    hz = hz,
                    isSelected = activeHz == hz,
                    onClick = {
                        if (!useRoot && !useShizuku) {
                            showNoPermDialog = true
                        } else {
                            activeHz = hz
                            refreshRateManager.setRefreshRate(hz, useRoot, useShizuku)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun MiuixRefreshRateCard(hz: Int, isSelected: Boolean, onClick: () -> Unit) {
    val accentColor = when {
        hz >= 144 -> Color(0xFF3478F6)
        hz >= 120 -> Color(0xFF5AC8FA)
        hz >= 90  -> Color(0xFF34C759)
        else      -> Color(0xFFFFCC00)
    }

    val cardBg by animateColorAsState(
        targetValue = if (isSelected) accentColor else MiuixTheme.colorScheme.surfaceContainer,
        label = "cardBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when {
                        hz >= 144 -> "超高刷"
                        hz >= 120 -> "高刷"
                        hz >= 90  -> "流畅"
                        else      -> "标准"
                    },
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.85f)
                    else MiuixTheme.colorScheme.onSurfaceVariantActions
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$hz Hz",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White
                    else MiuixTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = accentColor)
                }
            }
        }
    }
}