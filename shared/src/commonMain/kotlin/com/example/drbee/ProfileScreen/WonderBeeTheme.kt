package com.example.drbee.ProfileScreen

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.drbee.ProfileScreen.ThemePreferencesManager.isCustomColorEnabled

enum class WonderBeeThemeType {
    DEFAULT_LIGHT,
    WARM_HONEY,
    DARK_MINT
}

class WonderBeeCustomDesign(
    val surfaceBackground: Color,
    val textTitleBrush: Brush,
    val primaryGradientBrush: Brush,
    val inputFocusedBorderColor: Color,
    val inputUnfocusedBorderColor: Color,
    val inputLabelColor: Color
)

val LocalWonderBeeDesign = staticCompositionLocalOf<WonderBeeCustomDesign> {
    error("Extended Design System context not initialized.")
}

// Mock models to satisfy compilation boundaries across platforms
class FirebaseChatModel(val name: String = "", val email: String = "")
object Firebase {
    val auth = AuthMock()
    fun database(url: String) = DatabaseMock()
}
class AuthMock {
    val currentUser: UserMock? = UserMock()
    fun signOut() {}
}
class UserMock { val uid: String = "test_uid"; val email: String = "bee@hive.com" }
class DatabaseMock { fun reference(path: String) = ReferenceMock() }
class ReferenceMock { fun child(path: String) = ChildMock() }
class ChildMock { val valueEvents = kotlinx.coroutines.flow.flowOf(SnapshotMock()) }
class SnapshotMock { val exists: Boolean = true; inline fun <reified T> value(): T = FirebaseChatModel("Queen Bee", "bee@hive.com") as T }
object Napier { fun e(msg: String) {} }


@Composable
fun WonderBeeTheme(
    themeType: WonderBeeThemeType = ThemePreferencesManager.currentAppThemeSelection,
    customEnabled: Boolean = ThemePreferencesManager.isCustomColorEnabled,
    content: @Composable () -> Unit
) {
    val selectedScheme: ColorScheme
    val selectedDesign: WonderBeeCustomDesign

    val isDarkMode = themeType == WonderBeeThemeType.DARK_MINT

    val primaryColor = if (customEnabled) ThemePreferencesManager.customColorStart else {
        when (themeType) {
            WonderBeeThemeType.WARM_HONEY -> Color(0xFFE65100)
            WonderBeeThemeType.DARK_MINT -> Color(0xFF4DB6AC)
            else -> Color(0xFFD09200)
        }
    }

    val secondaryColor = if (customEnabled) ThemePreferencesManager.customColorEnd else {
        when (themeType) {
            WonderBeeThemeType.WARM_HONEY -> Color(0xFFFFB74D)
            WonderBeeThemeType.DARK_MINT -> Color(0xFF00796B)
            else -> Color(0xFFFFC107)
        }
    }

    if (isDarkMode) {
        selectedScheme = darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onBackground = Color(0xFFE0E0E0)
        )
        selectedDesign = WonderBeeCustomDesign(
            surfaceBackground = Color(0xFF121212), // Dark Mode Background color token
            textTitleBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
            primaryGradientBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
            inputFocusedBorderColor = primaryColor,
            inputUnfocusedBorderColor = Color(0xFF424242),
            inputLabelColor = primaryColor
        )
    } else {
        selectedScheme = lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = if (themeType == WonderBeeThemeType.WARM_HONEY) Color(0xFFFFF3E0) else Color(0xFFFFFBEA),
            surface = if (themeType == WonderBeeThemeType.WARM_HONEY) Color(0xFFFFE0B2) else Color.White,
            onPrimary = Color.White,
            onBackground = if (themeType == WonderBeeThemeType.WARM_HONEY) Color(0xFF3E2723) else Color(0xFF0F1A34)
        )

        selectedDesign = WonderBeeCustomDesign(
            surfaceBackground = if (themeType == WonderBeeThemeType.WARM_HONEY) Color(0xFFFFE0B2) else Color(0xFFECE5DD),
            textTitleBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
            primaryGradientBrush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
            inputFocusedBorderColor = primaryColor,
            inputUnfocusedBorderColor = Color.Gray,
            inputLabelColor = primaryColor
        )
    }

    CompositionLocalProvider(LocalWonderBeeDesign provides selectedDesign) {
        MaterialTheme(colorScheme = selectedScheme, content = content)
    }
}

object WonderBeeTheme {
    val extendedDesign: WonderBeeCustomDesign
        @Composable
        @ReadOnlyComposable
        get() = LocalWonderBeeDesign.current

    val materialScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme
}

