package com.example.drbee.MainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@Composable
fun SearchScreen(onInviteClicked: (String) -> Unit) {


    val currentUser by Firebase.auth.authStateChanged.collectAsState(
        initial = Firebase.auth.currentUser
    )
    val currentUserId = currentUser?.uid

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Search Screen", fontSize = 20.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onInviteClicked(currentUserId.toString()) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CA62))
        ) {
            Text(text = "Invite a Friend", color = Color.White)
        }
    }
}
