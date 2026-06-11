package com.example.drbee.MainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.drbee.Helper.NotificationEvent
import com.example.drbee.Helper.NotificationRouter
import com.example.drbee.ProfileScreen.ProfileScreen
import com.example.drbee.ProfileScreen.ReferralProfileSheet
import com.example.drbee.ProfileScreen.WonderBeeTheme

sealed class BottomTab(val route: String) {
    data object Home      : BottomTab("home")
    data object Search    : BottomTab("search")
    data object Community : BottomTab("community")
    data object Chat      : BottomTab("chat")
    data object Profile   : BottomTab("profile")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    navController             : NavController,
    onLogoutSuccess           : () -> Unit,
    // ── Deep-link referral popup ──────────────────────────────────────────
    pendingReferrerId         : String?  = null,
    onPendingReferrerConsumed : () -> Unit = {}
) {
    var selectedTab         by remember { mutableStateOf<BottomTab>(BottomTab.Community) }
    var isChatDetailOpen    by remember { mutableStateOf(false) }
    var targetOtherUserId   by remember { mutableStateOf<String?>(null) }
    var notificationVersion by remember { mutableIntStateOf(0) }

    // ── Referral popup state ──────────────────────────────────────────────
    // Once MainScreen is composed and a pendingReferrerId arrives, show the sheet.
    var activeReferrerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingReferrerId) {
        if (!pendingReferrerId.isNullOrBlank()) {
            activeReferrerId = pendingReferrerId
            onPendingReferrerConsumed()          // clear it upstream so rotation doesn't re-show
        }
    }

    // ── Notification routing (unchanged) ─────────────────────────────────
    val routerVersion = NotificationRouter.routerVersion

    LaunchedEffect(routerVersion) {
        if (routerVersion == 0) return@LaunchedEffect
        when (val event = NotificationRouter.consume()) {
            is NotificationEvent.OpenChat -> {
                targetOtherUserId   = event.otherUserId
                notificationVersion += 1
                selectedTab         = BottomTab.Chat
            }
            is NotificationEvent.OpenCommunity -> {
                selectedTab = BottomTab.Community
            }
            null -> { /* already consumed */ }
        }
    }

    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state           = backState,
        isBackEnabled   = true,
        onBackCancelled = {},
        onBackCompleted = {}
    )

    Scaffold(
        modifier       = Modifier
            .fillMaxSize()
            .background(WonderBeeTheme.extendedDesign.surfaceBackground),
        containerColor = WonderBeeTheme.extendedDesign.surfaceBackground,
        bottomBar      = {
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
                BottomTab.Home      -> HomeScreen()
                BottomTab.Search    -> SearchScreen()
                BottomTab.Community -> CommunityScreen()
                BottomTab.Profile   -> ProfileScreen(
                    navController   = navController,
                    onLogoutSuccess = onLogoutSuccess
                )
                BottomTab.Chat      -> ChatActivity(
                    targetOtherUserId        = targetOtherUserId,
                    notificationVersion      = notificationVersion,
                    onChatDetailStateChanged = { isOpen ->
                        isChatDetailOpen = isOpen
                    }
                )
            }
        }
    }

    // ── Instagram-style referral profile sheet ────────────────────────────
    // Rendered outside Scaffold so it overlays everything including the bottom bar.
    activeReferrerId?.let { referrerId ->
        ReferralProfileSheet(
            referrerId = referrerId,
            onDismiss  = { activeReferrerId = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BottomBar(selected: BottomTab, onSelect: (BottomTab) -> Unit) {
    val activeGradient  = WonderBeeTheme.extendedDesign.primaryGradientBrush
    val unselectedColor = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.4f)

    NavigationBar {
        BottomBarItem(BottomTab.Home,      selected, "Home",    Icons.Default.Home,        activeGradient, unselectedColor, onSelect)
        BottomBarItem(BottomTab.Search,    selected, "Search",  Icons.Default.Search,      activeGradient, unselectedColor, onSelect)
        BottomBarItem(BottomTab.Community, selected, "Today",   Icons.Default.Celebration, activeGradient, unselectedColor, onSelect)
        BottomBarItem(BottomTab.Chat,      selected, "Chat",    Icons.Default.Email,       activeGradient, unselectedColor, onSelect)
        BottomBarItem(BottomTab.Profile,   selected, "Profile", Icons.Default.Person,      activeGradient, unselectedColor, onSelect)
    }
}

@Composable
private fun RowScope.BottomBarItem(
    tab             : BottomTab,
    selected        : BottomTab,
    label           : String,
    icon            : androidx.compose.ui.graphics.vector.ImageVector,
    activeGradient  : Brush,
    unselectedColor : Color,
    onSelect        : (BottomTab) -> Unit
) {
    val isActive = selected == tab
    NavigationBarItem(
        selected = isActive,
        onClick  = { onSelect(tab) },
        icon     = {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                modifier           = if (isActive) Modifier.applyGradientTint(activeGradient) else Modifier,
                tint               = if (isActive) Color.White else unselectedColor
            )
        },
        label    = {
            Text(
                text     = label,
                modifier = if (isActive) Modifier.applyGradientTint(activeGradient) else Modifier,
                color    = if (isActive) Color.White else unselectedColor
            )
        },
        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
    )
}

fun Modifier.applyGradientTint(brush: Brush): Modifier = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.SrcIn)
    }