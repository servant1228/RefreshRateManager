package com.arcovery.refreshratemanager.ui.miuix

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import com.arcovery.refreshratemanager.utils.RefreshRateManager
import com.arcovery.refreshratemanager.utils.ShizukuHelper

@Composable
fun MiuixSettingsScreen(
    refreshRateManager: RefreshRateManager,
    useRoot: Boolean,
    useShizuku: Boolean,
    onRootToggle: (Boolean) -> Unit,
    onShizukuToggle: (Boolean) -> Unit,
    currentUiTheme: String,
    onUiThemeChange: (String) -> Unit,
    currentDarkMode: String,
    onDarkModeChange: (String) -> Unit,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val shizukuAlive by ShizukuHelper.isBinderAlive.collectAsState()
    val shizukuPermission by ShizukuHelper.isPermissionGranted.collectAsState()
    val shizukuConnected by ShizukuHelper.isServiceConnected.collectAsState()
    val rootSolution by remember { mutableStateOf(refreshRateManager.getRootSolution()) }

    var showRootUnavailableDialog by remember { mutableStateOf(false) }
    var showShizukuUnavailableDialog by remember { mutableStateOf(false) }

    // Root 弹窗
    OverlayDialog(
        show = showRootUnavailableDialog,
        title = "Root 不可用",
        onDismissRequest = { showRootUnavailableDialog = false }
    ) {
        Text("未检测到 Root 权限，请确保设备已 Root。")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = "知道了",
                onClick = { showRootUnavailableDialog = false }
            )
        }
    }

    // Shizuku 弹窗
    OverlayDialog(
        show = showShizukuUnavailableDialog,
        title = "Shizuku 不可用",
        onDismissRequest = { showShizukuUnavailableDialog = false }
    ) {
        Text("Shizuku 未运行或未授权，请先启动 Shizuku 应用并授权。")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = "知道了",
                onClick = { showShizukuUnavailableDialog = false }
            )
        }
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    // 关于弹窗
    OverlayDialog(
        show = showAboutDialog,
        title = "关于",
        onDismissRequest = { showAboutDialog = false }
    ) {
        Column {
            Text(
                text = "作者: servant1228",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "版本: v2.0.0",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "开源: GitHub · servant1228/RefreshRateManager",
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    text = "关闭",
                    onClick = { showAboutDialog = false }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            SwitchPreference(
                checked = useRoot,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val hasRoot = refreshRateManager.isRootAvailable()
                        if (hasRoot) {
                            onRootToggle(true)
                        } else {
                            showRootUnavailableDialog = true
                        }
                    } else {
                        onRootToggle(false)
                    }
                },
                title = "Root 模式",
                summary = "通过 $rootSolution 执行 SU 命令切换刷新率"
            )
            SwitchPreference(
                checked = useShizuku,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (!shizukuAlive || !shizukuPermission) {
                            showShizukuUnavailableDialog = true
                            ShizukuHelper.requestPermission()
                        } else {
                            ShizukuHelper.requestPermission()
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                if (ShizukuHelper.isServiceConnected.value) {
                                    onShizukuToggle(true)
                                }
                            }
                        }
                    } else {
                        onShizukuToggle(false)
                    }
                },
                title = "Shizuku 模式",
                summary = buildString {
                    append("通过 Shizuku 执行 ADB 命令")
                    if (useShizuku) {
                        append(" · ")
                        append(
                            when {
                                !shizukuAlive -> "Shizuku 未运行"
                                !shizukuPermission -> "未授权"
                                shizukuConnected -> "已连接"
                                else -> "连接中..."
                            }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            ArrowPreference(
                title = "UI 主题",
                summary = when (currentUiTheme) {
                    "miuix" -> "MiuiX"
                    "material3" -> "Material 3"
                    else -> currentUiTheme
                },
                onClick = {
                    val newTheme = if (currentUiTheme == "miuix") "material3" else "miuix"
                    onUiThemeChange(newTheme)
                }
            )
            ArrowPreference(
                title = "深色模式",
                summary = when (currentDarkMode) {
                    "system" -> "跟随系统"
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> currentDarkMode
                },
                onClick = {
                    val next = when (currentDarkMode) {
                        "system" -> "light"
                        "light" -> "dark"
                        "dark" -> "system"
                        else -> "system"
                    }
                    onDarkModeChange(next)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            ArrowPreference(
                title = "关于",
                summary = "servant1228 · v2.0.0",
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}