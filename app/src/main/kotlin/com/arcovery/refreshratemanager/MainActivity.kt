package com.arcovery.refreshratemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arcovery.refreshratemanager.ui.material3.M3MainScreen
import com.arcovery.refreshratemanager.ui.miuix.MiuixMainScreen
import com.arcovery.refreshratemanager.ui.theme.RefreshRateManagerM3Theme
import com.arcovery.refreshratemanager.utils.RefreshRateManager
import com.arcovery.refreshratemanager.utils.SettingsStore
import com.arcovery.refreshratemanager.utils.ShizukuHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class MainActivity : ComponentActivity() {

    private lateinit var refreshRateManager: RefreshRateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        refreshRateManager = RefreshRateManager(this)
        ShizukuHelper.init()

        setContent {
            AppRoot(refreshRateManager = refreshRateManager)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.release()
    }
}

@Composable
fun AppRoot(refreshRateManager: RefreshRateManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiTheme by SettingsStore.getUiTheme(context).collectAsState(initial = "miuix")
    val darkMode by SettingsStore.getDarkMode(context).collectAsState(initial = "system")
    val runMode by SettingsStore.getRunMode(context).collectAsState(initial = "root")

    val useRoot = runMode == "root"
    val useShizuku = runMode == "shizuku"

    val onRootToggle: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsStore.setRunMode(context, if (enabled) "root" else "none")
        }
    }

    val onShizukuToggle: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsStore.setRunMode(context, if (enabled) "shizuku" else "none")
        }
    }

    val onUiThemeChange: (String) -> Unit = { theme ->
        scope.launch { SettingsStore.setUiTheme(context, theme) }
    }

    val onDarkModeChange: (String) -> Unit = { mode ->
        scope.launch { SettingsStore.setDarkMode(context, mode) }
    }

    when (uiTheme) {
        "miuix" -> {
            val colorSchemeMode = when (darkMode) {
                "dark" -> ColorSchemeMode.Dark
                "light" -> ColorSchemeMode.Light
                else -> ColorSchemeMode.System
            }

            MiuixTheme(
                controller = remember(colorSchemeMode) { ThemeController(colorSchemeMode) }
            ) {
                MiuixMainScreen(
                    refreshRateManager = refreshRateManager,
                    useRoot = useRoot,
                    useShizuku = useShizuku,
                    onRootToggle = onRootToggle,
                    onShizukuToggle = onShizukuToggle,
                    currentUiTheme = uiTheme,
                    onUiThemeChange = onUiThemeChange,
                    currentDarkMode = darkMode,
                    onDarkModeChange = onDarkModeChange
                )
            }
        }
        "material3" -> {
            RefreshRateManagerM3Theme(darkMode = darkMode) {
                M3MainScreen(
                    refreshRateManager = refreshRateManager,
                    useRoot = useRoot,
                    useShizuku = useShizuku,
                    onRootToggle = onRootToggle,
                    onShizukuToggle = onShizukuToggle,
                    currentUiTheme = uiTheme,
                    onUiThemeChange = onUiThemeChange,
                    currentDarkMode = darkMode,
                    onDarkModeChange = onDarkModeChange
                )
            }
        }
    }
}
