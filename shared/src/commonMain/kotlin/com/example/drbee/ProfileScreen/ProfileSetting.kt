package com.example.drbee.ProfileScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

data class ProfileMenu(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun profileScreenKMP(
    onEditProfile: () -> Unit = {}
) {

    val menuItems = listOf(
        ProfileMenu("My Booking", Icons.Default.ConfirmationNumber),
        ProfileMenu("Movie Alerts", Icons.Default.Notifications),
        ProfileMenu("Preference", Icons.Default.Tune),
        ProfileMenu("Privacy Policy", Icons.Default.Shield),
        ProfileMenu("Contact Us", Icons.Default.ContactMail),
        ProfileMenu("Logout", Icons.Default.Logout)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // TOP BLUE HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0057C2),
                                Color(0xFF006EEA)
                            )
                        )
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(70.dp))

            Text(
                text = "Tara Jain",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "tarajain18@google.com",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {

                items(menuItems) { item ->

                    Card(
                        modifier = Modifier
                            .height(110.dp)
                            .clickable { },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = cardElevation(4.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color(0xFF0057C2),
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                color = Color(0xFF0057C2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "MADE WITH ",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = "❤",
                    fontSize = 28.sp,
                    color = Color.Red
                )

                Text(
                    text = " IN INDIA",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(90.dp))
        }

        // PROFILE IMAGE
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {

            AsyncImage(
                model = "https://i.pravatar.cc/300",
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0057C2)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // BOTTOM NAVIGATION
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            containerColor = Color.White
        ) {

            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = {
                    Icon(Icons.Default.Home, contentDescription = null)
                }
            )

            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )

            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0057C2)),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            Icons.Default.GridView,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            )

            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )

            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = {
                    Icon(Icons.Default.PersonOutline, contentDescription = null)
                }
            )
        }
    }
}