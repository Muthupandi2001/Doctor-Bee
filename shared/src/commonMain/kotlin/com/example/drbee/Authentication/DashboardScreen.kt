package com.example.drbee.Authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.Helper.BeeWarmIvory
import com.example.drbee.Helper.BeeBrightYellow
import com.example.drbee.Helper.BeeDarkNavy
import com.example.drbee.Helper.BeeWarmIvory
import com.example.drbee.Helper.HexagonShape

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeWarmIvory)
    ) {
        // Upper Profile Greetings Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Hello,", fontSize = 28.sp, fontWeight = FontWeight.Light, color = BeeDarkNavy)
                Text(text = "Queen Bee!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BeeDarkNavy)
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(BeeBrightYellow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Face, contentDescription = "Queen Profile", modifier = Modifier.size(40.dp), tint = BeeDarkNavy)
            }
        }

        // Summary Metric Ribbon Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BeeDarkNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem("Active Hives", "5", Icons.Default.Home)
                MetricItem("Nectar Collected", "250 L", Icons.Default.WaterDrop)
                MetricItem("Queen Health", "Excellent", Icons.Default.Favorite)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Hive Custom Hexagonal Activity Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    HiveHexagonUnit("Field Workers")
                    Spacer(modifier = Modifier.width(4.dp))
                    HiveHexagonUnit("Brood Care")
                }
                Row(modifier = Modifier.offset(y = (-15).dp)) {
                    HiveHexagonUnit("Honey Processing")
                    Spacer(modifier = Modifier.width(4.dp))
                    HiveHexagonUnit("Queen Room")
                }
            }
        }
    }
}

@Composable
fun RowScope.MetricItem(title: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = BeeBrightYellow, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, fontSize = 11.sp, color = Color.LightGray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun HiveHexagonUnit(label: String) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(HexagonShape)
            .background(BeeBrightYellow)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Star, contentDescription = null, tint = BeeDarkNavy, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BeeDarkNavy)
        }
    }
}