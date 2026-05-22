package com.example.drbee.MainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import theme.AppColors
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.drbee.ChatActivity.ChatActivity
import com.example.drbee.ProfileScreen.ProfileScreen
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme
import com.example.drbee.Routes

sealed class BottomTab(val route: String) {
    data object Home : BottomTab("home")
    data object Search : BottomTab("search")
    data object Camera : BottomTab("camera")
    data object Chat : BottomTab("chat")
    data object Profile : BottomTab("profile")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(navController: NavController,onShareRequested: (String) -> Unit) {

    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }

    val navigationEventState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None
    )

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = true, // You can toggle this dynamically using your state
        onBackCancelled = {
            // Optional: User started swipe back gesture but aborted it
        },
        onBackCompleted = {
            // Put your actual back-press handling logic here
            println("Back custom action executed!")
        }
    )


    Scaffold(
        modifier = Modifier.fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground),
        containerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
        bottomBar = {
            BottomBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.padding(padding)) {

            when (selectedTab) {

                BottomTab.Home -> HomeScreen()

                BottomTab.Search ->  SearchScreen(onInviteClicked = onShareRequested)

                BottomTab.Camera -> CameraScreen()

                BottomTab.Chat -> ChatActivity()

                BottomTab.Profile -> ProfileScreen(
                    onLogoutSuccess = {
                        // Clear all pages and send user back to login form safely
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

fun Modifier.applyGradientTint(brush: Brush): Modifier = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.SrcIn)
    }

@Composable
fun BottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit
) {
    val activeGradient = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val unselectedColor = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)

    NavigationBar {

        NavigationBarItem(
            selected = selected == BottomTab.Home,
            onClick = { onSelect(BottomTab.Home) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = if (selected == BottomTab.Home) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    tint = if (selected == BottomTab.Home) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text = "Home",
                    modifier = if (selected == BottomTab.Home) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    color = if (selected == BottomTab.Home) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = selected == BottomTab.Search,
            onClick = { onSelect(BottomTab.Search) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = if (selected == BottomTab.Search) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    tint = if (selected == BottomTab.Search) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text = "Search",
                    modifier = if (selected == BottomTab.Search) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    color = if (selected == BottomTab.Search) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // ⭐ CENTER SPECIAL BUTTON (Camera)
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            val fabGradientBrush = if (ThemePreferencesManager.isCustomColorEnabled) {
                Brush.horizontalGradient(
                    colors = listOf(ThemePreferencesManager.customColorStart, ThemePreferencesManager.customColorEnd)
                )
            } else {
                // Fallback layout wraps your design system theme palette variables automatically
                Brush.horizontalGradient(
                    colors = listOf(WonderBeeTheme.materialScheme.primary, WonderBeeTheme.materialScheme.secondary)
                )
            }

            FloatingActionButton(
                onClick = { onSelect(BottomTab.Camera) },
                modifier = Modifier
                    .size(52.dp)
                    // 2. Inject the horizontal brush gradient configuration into the layout background
                    .background(brush = fabGradientBrush, shape = CircleShape),
                // 3. Clear containerColor to transparent so the background gradient shines through
                containerColor = Color.Transparent,
                contentColor = WonderBeeTheme.materialScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp), // Disables flat M3 shadow artifacts over your custom layer
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Camera",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        NavigationBarItem(
            selected = selected == BottomTab.Chat,
            onClick = { onSelect(BottomTab.Chat) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = if (selected == BottomTab.Chat) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    tint = if (selected == BottomTab.Chat) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text = "Chat",
                    modifier = if (selected == BottomTab.Chat) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    color = if (selected == BottomTab.Chat) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = selected == BottomTab.Profile,
            onClick = { onSelect(BottomTab.Profile) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = if (selected == BottomTab.Profile) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    tint = if (selected == BottomTab.Profile) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text = "Profile",
                    modifier = if (selected == BottomTab.Profile) Modifier.applyGradientTint(
                        activeGradient
                    ) else Modifier,
                    color = if (selected == BottomTab.Profile) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}