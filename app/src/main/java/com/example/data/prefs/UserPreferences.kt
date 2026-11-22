package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettingsState
import com.example.data.model.FontSizePreference
import com.example.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "alok_chat_settings")

class UserPreferences(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val FONT_SIZE = stringPreferencesKey("font_size_pref")
        val ENTER_IS_SEND = booleanPreferencesKey("enter_is_send")
        val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val NOTIF_VIBRATE = booleanPreferencesKey("notif_vibrate")
        val NOTIF_PREVIEW = booleanPreferencesKey("notif_preview")
        val AUTO_DL_WIFI = booleanPreferencesKey("auto_dl_wifi")
        val AUTO_DL_CELL = booleanPreferencesKey("auto_dl_cell")
        val WALLPAPER_IDX = intPreferencesKey("wallpaper_idx")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME]?.let { raw ->
            ThemePreference.entries.firstOrNull { it.name == raw }
        } ?: ThemePreference.SYSTEM

        val fontSize = prefs[Keys.FONT_SIZE]?.let { raw ->
            FontSizePreference.entries.firstOrNull { it.name == raw }
        } ?: FontSizePreference.MEDIUM

        AppSettingsState(
            themePreference = theme,
            fontSizePreference = fontSize,
            enterIsSend = prefs[Keys.ENTER_IS_SEND] ?: false,
            notificationSound = prefs[Keys.NOTIF_SOUND] ?: true,
            notificationVibrate = prefs[Keys.NOTIF_VIBRATE] ?: true,
            notificationShowPreview = prefs[Keys.NOTIF_PREVIEW] ?: true,
            autoDownloadWifi = prefs[Keys.AUTO_DL_WIFI] ?: true,
            autoDownloadCellular = prefs[Keys.AUTO_DL_CELL] ?: false,
            chatWallpaperIndex = prefs[Keys.WALLPAPER_IDX] ?: 0
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setFontSize(fontSize: FontSizePreference) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = fontSize.name }
    }

    suspend fun setEnterIsSend(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENTER_IS_SEND] = enabled }
    }

    suspend fun setNotificationSound(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_SOUND] = enabled }
    }

    suspend fun setNotificationVibrate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_VIBRATE] = enabled }
    }

    suspend fun setNotificationShowPreview(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIF_PREVIEW] = enabled }
    }

    suspend fun setAutoDownloadWifi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DL_WIFI] = enabled }
    }

    suspend fun setAutoDownloadCellular(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DL_CELL] = enabled }
    }

    suspend fun setChatWallpaper(index: Int) {
        context.dataStore.edit { it[Keys.WALLPAPER_IDX] = index }
    }
}
