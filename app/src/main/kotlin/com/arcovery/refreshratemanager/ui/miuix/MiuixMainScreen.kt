package com.arcovery.refreshratemanager.ui.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.arcovery.refreshratemanager.utils.RefreshRateManager
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

@Composable
fun MiuixMainScreen(
    refreshRateManager: RefreshRateManager,
    useRoot: Boolean,
    useShizuku: Boolean,
    onRootToggle: (Boolean) -> Unit,
    onShizukuToggle: (Boolean) -> Unit,
    currentUiTheme: String,
    onUiThemeChange: (String) -> Unit,
    currentDarkMode: String,
    onDarkModeChange: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = when (selectedTab) {
                    0 -> "刷新率管理"
                    1 -> "设置"
                    else -> "刷新率管理"
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = Icons.Outlined.Tune,
                    label = "配置"
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = Icons.Outlined.Settings,
                    label = "设置"
                )
            }
        },
        content = { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> MiuixConfigScreen(
                        refreshRateManager = refreshRateManager,
                        useRoot = useRoot,
                        useShizuku = useShizuku,
                        paddingValues = paddingValues
                    )
                    1 -> MiuixSettingsScreen(
                        refreshRateManager = refreshRateManager,
                        useRoot = useRoot,
                        useShizuku = useShizuku,
                        onRootToggle = onRootToggle,
                        onShizukuToggle = onShizukuToggle,
                        currentUiTheme = currentUiTheme,
                        onUiThemeChange = onUiThemeChange,
                        currentDarkMode = currentDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        paddingValues = paddingValues
                    )
                }
            }
        }
    )
}
