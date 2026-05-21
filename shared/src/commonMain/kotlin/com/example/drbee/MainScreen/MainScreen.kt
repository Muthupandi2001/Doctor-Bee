package com.example.drbee.MainScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import theme.AppColors
import androidx.compose.ui.backhandler.BackHandler
import com.example.drbee.ChatActivity.ChatActivity

sealed class BottomTab(val route: String) {
    data object Home : BottomTab("home")
    data object Search : BottomTab("search")
    data object Camera : BottomTab("camera")
    data object Chat : BottomTab("chat")
    data object Profile : BottomTab("profile")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {

    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Home) }
    BackHandler(enabled = true) {

    }

    Scaffold(
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

                BottomTab.Search -> SearchScreen()

                BottomTab.Camera -> CameraScreen()

                BottomTab.Chat -> ChatActivity()

                BottomTab.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
fun BottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit
) {

    NavigationBar {

        NavigationBarItem(
            selected = selected == BottomTab.Home,
            onClick = { onSelect(BottomTab.Home) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = selected == BottomTab.Search,
            onClick = { onSelect(BottomTab.Search) },
            icon = { Icon(Icons.Default.Search, null) },
            label = { Text("Search") }
        )

        // ⭐ CENTER SPECIAL BUTTON (Camera)
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { onSelect(BottomTab.Camera) },
                containerColor = AppColors.Green
            ) {
                Icon(Icons.Default.Add, contentDescription = "Camera")
            }
        }

        NavigationBarItem(
            selected = selected == BottomTab.Chat,
            onClick = { onSelect(BottomTab.Chat) },
            icon = { Icon(Icons.Default.Email, null) },
            label = { Text("Chat") }
        )

        NavigationBarItem(
            selected = selected == BottomTab.Profile,
            onClick = { onSelect(BottomTab.Profile) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") }
        )
    }
}