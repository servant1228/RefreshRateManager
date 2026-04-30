package com.arcovery.refreshratemanager.ui.material3

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.arcovery.refreshratemanager.utils.RefreshRateManager
import com.arcovery.refreshratemanager.utils.ShizukuHelper

@Composable
fun M3SettingsScreen(
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

    var showRootUnavailableDialog by remember { mutableStateOf(false) }
    var showShizukuUnavailableDialog by remember { mutableStateOf(false) }

    if (showRootUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showRootUnavailableDialog = false },
            title = { Text("Root 不可用") },
            text = { Text("未检测到 Root 权限，请确保设备已 Root。") },
            confirmButton = {
                TextButton(onClick = { showRootUnavailableDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showShizukuUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuUnavailableDialog = false },
            title = { Text("Shizuku 不可用") },
            text = { Text("Shizuku 未运行或未授权，请先启动 Shizuku 应用并授权。") },
            confirmButton = {
                TextButton(onClick = { showShizukuUnavailableDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于") },
            text = {
                Column {
                    Text("作者: servant1228")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("版本: v2.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("开源: GitHub · servant1228/RefreshRateManager")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            val rootSolution = remember { refreshRateManager.getRootSolution() }

            ListItem(
                headlineContent = { Text("Root 模式") },
                supportingContent = { Text("通过 $rootSolution 执行 SU 命令切换刷新率") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Root",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = useRoot,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (refreshRateManager.isRootAvailable()) {
                                    onRootToggle(true)
                                } else {
                                    showRootUnavailableDialog = true
                                }
                            } else {
                                onRootToggle(false)
                            }
                        }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text("Shizuku 模式") },
                supportingContent = {
                    Text(
                        buildString {
                            append("通过 Shizuku ADB 执行命令")
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
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Shizuku",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = useShizuku,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!shizukuAlive || !shizukuPermission) {
                                    showShizukuUnavailableDialog = true
                                }
                                ShizukuHelper.requestPermission()
                                scope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    if (ShizukuHelper.isServiceConnected.value) {
                                        onShizukuToggle(true)
                                    }
                                }
                            } else {
                                onShizukuToggle(false)
                            }
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("UI 主题") },
                supportingContent = {
                    Text(
                        when (currentUiTheme) {
                            "miuix" -> "MiuiX"
                            "material3" -> "Material 3"
                            else -> currentUiTheme
                        }
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                trailingContent = {
                    FilledTonalButton(
                        onClick = {
                            val newTheme = if (currentUiTheme == "miuix") "material3" else "miuix"
                            onUiThemeChange(newTheme)
                        }
                    ) {
                        Text("切换")
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = {
                    Text(
                        when (currentDarkMode) {
                            "system" -> "跟随系统"
                            "light" -> "浅色"
                            "dark" -> "深色"
                            else -> currentDarkMode
                        }
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = "DarkMode",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                trailingContent = {
                    FilledTonalButton(
                        onClick = {
                            val next = when (currentDarkMode) {
                                "system" -> "light"
                                "light" -> "dark"
                                "dark" -> "system"
                                else -> "system"
                            }
                            onDarkModeChange(next)
                        }
                    ) {
                        Text(
                            when (currentDarkMode) {
                                "system" -> "跟随系统"
                                "light" -> "浅色"
                                "dark" -> "深色"
                                else -> "跟随系统"
                            }
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text("关于") },
                supportingContent = { Text("servant1228 · v2.0.0") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
