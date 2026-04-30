package com.arcovery.refreshratemanager.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsStore {
    // UI Theme: "miuix" or "material3"
    private val UI_THEME = stringPreferencesKey("ui_theme")
    // Dark mode: "system", "light", "dark"
    private val DARK_MODE = stringPreferencesKey("dark_mode")
    // Run mode: "root" or "shizuku"
    private val RUN_MODE = stringPreferencesKey("run_mode")
    // Last selected refresh rate
    private val LAST_REFRESH_RATE = intPreferencesKey("last_refresh_rate")
    // Bottom bar style: "default" or "floating"
    private val BOTTOM_BAR_STYLE = stringPreferencesKey("bottom_bar_style")

    fun getUiTheme(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[UI_THEME] ?: "miuix"
        }
    }

    suspend fun setUiTheme(context: Context, theme: String) {
        context.dataStore.edit { prefs ->
            prefs[UI_THEME] = theme
        }
    }

    fun getDarkMode(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[DARK_MODE] ?: "system"
        }
    }

    suspend fun setDarkMode(context: Context, mode: String) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE] = mode
        }
    }

    fun getRunMode(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[RUN_MODE] ?: "none"
        }
    }

    suspend fun setRunMode(context: Context, mode: String) {
        context.dataStore.edit { prefs ->
            prefs[RUN_MODE] = mode
        }
    }

    fun getLastRefreshRate(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[LAST_REFRESH_RATE] ?: -1
        }
    }

    suspend fun setLastRefreshRate(context: Context, hz: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_REFRESH_RATE] = hz
        }
    }

    fun getBottomBarStyle(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[BOTTOM_BAR_STYLE] ?: "default"
        }
    }

    suspend fun setBottomBarStyle(context: Context, style: String) {
        context.dataStore.edit { prefs ->
            prefs[BOTTOM_BAR_STYLE] = style
        }
    }
}
