// commonMain/.../SearchScreen.kt
package com.example.drbee.MainScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.SessionManager
import com.example.drbee.shareReferralLink   // ✅ expect fun — no lambda needed
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@Composable
fun SearchScreen() {
    // ✅ onInviteClicked param removed — shareReferralLink called directly

    val currentUser by Firebase.auth.authStateChanged.collectAsState(
        initial = Firebase.auth.currentUser
    )

    val currentUserId = currentUser?.uid
        ?.takeUnless { it.isEmpty() }
        ?: SessionManager.savedUserId.takeUnless { it.isEmpty() }
        ?: ""

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Search Screen", fontSize = 20.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { shareReferralLink(currentUserId) },   // ✅ direct call
            colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CA62))
        ) {
            Text(text = "Invite a Friend", color = Color.White)
        }
    }
}