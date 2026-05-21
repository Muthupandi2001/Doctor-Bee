package com.example.drbee.ProfileScreen

import com.russhwolf.settings.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf


object ThemePreferencesManager {
    private val settings: Settings = Settings()

    private const val KEY_THEME_TYPE = "theme_type"
    private const val KEY_IS_CUSTOM_COLOR = "is_custom_color_enabled"
    private const val KEY_CURSOR_X1 = "cursor_x1"
    private const val KEY_CURSOR_Y1 = "cursor_y1"
    private const val KEY_CURSOR_X2 = "cursor_x2"
    private const val KEY_CURSOR_Y2 = "cursor_y2"

    // Singleton state controllers tracking layout profiles
    var currentAppThemeSelection by mutableStateOf(WonderBeeThemeType.DEFAULT_LIGHT)
    var isCustomColorEnabled by mutableStateOf(false)
    var customColorStart by mutableStateOf(Color(0xFFFFD54F))
    var customColorEnd by mutableStateOf(Color(0xFFFF8F00))

    var savedX1 = 0.15f
    var savedY1 = 0.20f
    var savedX2 = 0.85f
    var savedY2 = 0.20f

    fun getColorFrom2DMatrix(pctX: Float, pctY: Float): Color {
        val normalizedX = pctX.coerceIn(0f, 1f)
        val normalizedY = pctY.coerceIn(0f, 1f)

        val section = normalizedX * 6f
        val index = section.toInt()
        val fraction = section - index

        val baseColor = when (index) {
            0 -> Color(1f, fraction, 0f)
            1 -> Color(1f - fraction, 1f, 0f)
            2 -> Color(0f, 1f, fraction)
            3 -> Color(0f, 1f - fraction, 1f)
            4 -> Color(fraction, 0f, 1f)
            else -> Color(1f, 0f, 1f - fraction)
        }

        return when {
            normalizedY < 0.5f -> {
                val mixFactor = 1f - (normalizedY * 2f)
                Color(
                    red = baseColor.red + (1f - baseColor.red) * mixFactor,
                    green = baseColor.green + (1f - baseColor.green) * mixFactor,
                    blue = baseColor.blue + (1f - baseColor.blue) * mixFactor
                )
            }
            else -> {
                val mixFactor = (normalizedY - 0.5f) * 2f
                Color(
                    red = baseColor.red * (1f - mixFactor),
                    green = baseColor.green * (1f - mixFactor),
                    blue = baseColor.blue * (1f - mixFactor)
                )
            }
        }
    }

    fun saveOnlyThemeType(themeType: WonderBeeThemeType) {
        settings.putString(KEY_THEME_TYPE, themeType.name)
    }

    fun saveThemeState(themeType: WonderBeeThemeType, useCustomColor: Boolean, x1: Float, y1: Float, x2: Float, y2: Float) {
        savedX1 = x1; savedY1 = y1; savedX2 = x2; savedY2 = y2
        settings.putString(KEY_THEME_TYPE, themeType.name)
        settings.putBoolean(KEY_IS_CUSTOM_COLOR, useCustomColor)
        settings.putFloat(KEY_CURSOR_X1, x1)
        settings.putFloat(KEY_CURSOR_Y1, y1)
        settings.putFloat(KEY_CURSOR_X2, x2)
        settings.putFloat(KEY_CURSOR_Y2, y2)
    }

    fun loadThemeSettings(onCoordinatesLoaded: (Float, Float, Float, Float) -> Unit) {
        val savedThemeName = settings.getString(KEY_THEME_TYPE, WonderBeeThemeType.DEFAULT_LIGHT.name)
        currentAppThemeSelection = try {
            WonderBeeThemeType.valueOf(savedThemeName)
        } catch (e: Exception) {
            WonderBeeThemeType.DEFAULT_LIGHT
        }

        isCustomColorEnabled = settings.getBoolean(KEY_IS_CUSTOM_COLOR, false)

        savedX1 = settings.getFloat(KEY_CURSOR_X1, 0.15f)
        savedY1 = settings.getFloat(KEY_CURSOR_Y1, 0.20f)
        savedX2 = settings.getFloat(KEY_CURSOR_X2, 0.85f)
        savedY2 = settings.getFloat(KEY_CURSOR_Y2, 0.20f)

        customColorStart = getColorFrom2DMatrix(savedX1, savedY1)
        customColorEnd = getColorFrom2DMatrix(savedX2, savedY2)

        onCoordinatesLoaded(savedX1, savedY1, savedX2, savedY2)
    }
}