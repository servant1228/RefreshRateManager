package com.arcovery.refreshratemanager.ui.material3

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arcovery.refreshratemanager.utils.RefreshRateManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3MainScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "刷新率管理"
                            1 -> "设置"
                            else -> "刷新率管理"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Tune else Icons.Outlined.Tune,
                            contentDescription = "配置"
                        )
                    },
                    label = { Text("配置") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "设置"
                        )
                    },
                    label = { Text("设置") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> M3ConfigScreen(
                refreshRateManager = refreshRateManager,
                useRoot = useRoot,
                useShizuku = useShizuku,
                paddingValues = paddingValues
            )
            1 -> M3SettingsScreen(
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
