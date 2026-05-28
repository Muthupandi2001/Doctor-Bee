package com.example.drbee.MainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.drbee.ChatActivity.ChatActivity
import com.example.drbee.CommunityScreen.CommunityScreen
import com.example.drbee.ProfileScreen.ProfileScreen
import com.example.drbee.ProfileScreen.ThemePreferencesManager
import com.example.drbee.ProfileScreen.WonderBeeTheme

sealed class BottomTab(val route: String) {
    data object Home    : BottomTab("home")
    data object Search  : BottomTab("search")
    data object Camera  : BottomTab("camera")
    data object Chat    : BottomTab("chat")
    data object Profile : BottomTab("profile")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    navController   : NavController,
    onLogoutSuccess : () -> Unit
    // ✅ onShareRequested, onPickImageRequested, onDecodeImageRequested all removed
) {
    var selectedTab      by remember { mutableStateOf<BottomTab>(BottomTab.Camera) }
    var isChatDetailOpen by remember { mutableStateOf(false) }

    val navigationEventState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None
    )
    NavigationBackHandler(
        state           = navigationEventState,
        isBackEnabled   = true,
        onBackCancelled = {},
        onBackCompleted = { }
    )

    Scaffold(
        modifier       = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground),
        containerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
        bottomBar = {
            if (!isChatDetailOpen) {
                BottomBar(selected = selectedTab, onSelect = { selectedTab = it })
            }
        }
    ) { padding ->
        Box(
            modifier = if (isChatDetailOpen) Modifier.fillMaxSize()
            else Modifier.padding(padding)
        ) {
            when (selectedTab) {
                BottomTab.Home    -> HomeScreen()
                BottomTab.Search  -> SearchScreen()
                BottomTab.Camera  -> CommunityScreen()
                BottomTab.Profile -> ProfileScreen(
                    navController   = navController,
                    onLogoutSuccess = onLogoutSuccess
                )
                BottomTab.Chat -> ChatActivity(
                    onChatDetailStateChanged = { isOpen -> isChatDetailOpen = isOpen }
                    // ✅ no image lambdas
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
    selected : BottomTab,
    onSelect : (BottomTab) -> Unit
) {
    val activeGradient  = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val unselectedColor = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)

    NavigationBar {

        NavigationBarItem(
            selected = selected == BottomTab.Home,
            onClick  = { onSelect(BottomTab.Home) },
            icon = {
                Icon(
                    imageVector        = Icons.Default.Home,
                    contentDescription = null,
                    modifier           = if (selected == BottomTab.Home)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    tint               = if (selected == BottomTab.Home) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text     = "Home",
                    modifier = if (selected == BottomTab.Home)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    color    = if (selected == BottomTab.Home) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = selected == BottomTab.Search,
            onClick  = { onSelect(BottomTab.Search) },
            icon = {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = null,
                    modifier           = if (selected == BottomTab.Search)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    tint               = if (selected == BottomTab.Search) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text     = "Search",
                    modifier = if (selected == BottomTab.Search)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    color    = if (selected == BottomTab.Search) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // Center FAB
        Box(
            modifier         = Modifier.padding(6.dp).size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            val fabBrush = if (ThemePreferencesManager.isCustomColorEnabled) {
                Brush.horizontalGradient(listOf(
                    ThemePreferencesManager.customColorStart,
                    ThemePreferencesManager.customColorEnd
                ))
            } else {
                Brush.horizontalGradient(listOf(
                    WonderBeeTheme.materialScheme.primary,
                    WonderBeeTheme.materialScheme.secondary
                ))
            }

            FloatingActionButton(
                onClick        = { onSelect(BottomTab.Camera) },
                modifier       = Modifier
                    .size(52.dp)
                    .background(brush = fabBrush, shape = CircleShape),
                containerColor = Color.Transparent,
                contentColor   = WonderBeeTheme.materialScheme.onPrimary,
                elevation      = FloatingActionButtonDefaults.elevation(0.dp),
                shape          = CircleShape
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Camera",
                    modifier           = Modifier.size(28.dp)
                )
            }
        }

        NavigationBarItem(
            selected = selected == BottomTab.Chat,
            onClick  = { onSelect(BottomTab.Chat) },
            icon = {
                Icon(
                    imageVector        = Icons.Default.Email,
                    contentDescription = null,
                    modifier           = if (selected == BottomTab.Chat)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    tint               = if (selected == BottomTab.Chat) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text     = "Chat",
                    modifier = if (selected == BottomTab.Chat)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    color    = if (selected == BottomTab.Chat) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = selected == BottomTab.Profile,
            onClick  = { onSelect(BottomTab.Profile) },
            icon = {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    modifier           = if (selected == BottomTab.Profile)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    tint               = if (selected == BottomTab.Profile) Color.White else unselectedColor
                )
            },
            label = {
                Text(
                    text     = "Profile",
                    modifier = if (selected == BottomTab.Profile)
                        Modifier.applyGradientTint(activeGradient) else Modifier,
                    color    = if (selected == BottomTab.Profile) Color.White else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}