package com.example.tournote.Groups

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val THEME_KEY = "app_theme"

    const val LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK = AppCompatDelegate.MODE_NIGHT_YES
    const val SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(THEME_KEY, SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun saveTheme(context: Context, themeMode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(THEME_KEY, themeMode)
            .apply()
    }
}
