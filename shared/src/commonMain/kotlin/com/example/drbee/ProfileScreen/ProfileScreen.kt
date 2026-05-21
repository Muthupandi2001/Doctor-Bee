package com.example.drbee.ProfileScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drbee.ChatActivity.FirebaseChatModel
import com.example.drbee.ProfileScreen.ThemePreferencesManager.isCustomColorEnabled
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.database.database
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@Composable
fun ProfileScreen(onLogoutSuccess: () -> Unit) {
    val auth = remember { Firebase.auth }
    val scope = rememberCoroutineScope()
    val currentUid = remember { auth.currentUser?.uid ?: "" }

    val databaseUrl = "https://doctor-bee-2d622-default-rtdb.firebaseio.com/"

    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }

    val masterScrollState = rememberScrollState()

    LaunchedEffect(currentUid) {
        if (currentUid.isBlank()) {
            userName = "Anonymous User"
            userEmail = "Not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            Firebase.database(databaseUrl)
                .reference("users")
                .child(currentUid)
                .valueEvents
                .collect { snapshot ->
                    if (snapshot.exists) {
                        val profileData = snapshot.value<FirebaseChatModel>()
                        userName = profileData.name.ifBlank { "No Name Provided" }
                        userEmail = profileData.email.ifBlank { auth.currentUser?.email ?: "" }
                    } else {
                        userName = "Hive Member"
                        userEmail = auth.currentUser?.email ?: "No Email Attached"
                    }
                    isLoading = false
                }
        } catch (e: Exception) {
            Napier.e("Profile credential loading failure: ${e.message}")
            userName = "Offline Mode"
            userEmail = auth.currentUser?.email ?: "Offline"
            isLoading = false
        }
    }

    // ✅ FIXED: Root content block now explicitly wraps inside the reactive dynamic Theme system provider
    WonderBeeTheme(
        themeType = ThemePreferencesManager.currentAppThemeSelection,
        customEnabled = ThemePreferencesManager.isCustomColorEnabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WonderBeeTheme.extendedDesign.surfaceBackground)
                .verticalScroll(masterScrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(WonderBeeTheme.materialScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = WonderBeeTheme.materialScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(color = WonderBeeTheme.materialScheme.primary)
            } else {
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WonderBeeTheme.materialScheme.onBackground
                )
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = WonderBeeTheme.materialScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "App Theme Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WonderBeeTheme.materialScheme.onBackground)
            }

            ProfileThemeOptionRow("Classic WonderBee Light", "Traditional gold and warm ivory look", WonderBeeThemeType.DEFAULT_LIGHT)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileThemeOptionRow("Warm Honey Glow", "Rich amber tones for soft reading comfort", WonderBeeThemeType.WARM_HONEY)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileThemeOptionRow("Midnight Mint Obsidian", "Battery-saving dark mode style configuration", WonderBeeThemeType.DARK_MINT)

            Spacer(modifier = Modifier.height(16.dp))

            FullSpectrumColorPickerPanel()

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            auth.signOut()
                            onLogoutSuccess()
                        } catch (e: Exception) {
                            Napier.e("Sign out runtime failure: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Logout From Hive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// =====================================================================
// 5. PRESET APP THEME OPTIONS ROW VIEW SELECTION (FIXED TO PRESERVE COLORS)
// =====================================================================


@Composable
fun ProfileThemeOptionRow(title: String, description: String, targetTheme: WonderBeeThemeType) {
    val isSelected = ThemePreferencesManager.currentAppThemeSelection == targetTheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                ThemePreferencesManager.currentAppThemeSelection = targetTheme
                ThemePreferencesManager.saveOnlyThemeType(targetTheme)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WonderBeeTheme.materialScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WonderBeeTheme.materialScheme.onBackground)
                Text(text = description, fontSize = 11.sp, color = WonderBeeTheme.materialScheme.onBackground.copy(alpha = 0.6f))
            }
            RadioButton(
                selected = isSelected,
                onClick = {
                    ThemePreferencesManager.currentAppThemeSelection = targetTheme
                    ThemePreferencesManager.saveOnlyThemeType(targetTheme)
                },
                colors = RadioButtonDefaults.colors(selectedColor = WonderBeeTheme.materialScheme.primary)
            )
        }
    }
}

@Composable
fun FullSpectrumColorPickerPanel() {
    var selectingFirstColor by remember { mutableStateOf(true) }

    var cursorPositionX1 by remember { mutableStateOf(0.15f) }
    var cursorPositionY1 by remember { mutableStateOf(0.20f) }

    var cursorPositionX2 by remember { mutableStateOf(0.85f) }
    var cursorPositionY2 by remember { mutableStateOf(0.20f) }

    LaunchedEffect(Unit) {
        ThemePreferencesManager.loadThemeSettings { x1, y1, x2, y2 ->
            cursorPositionX1 = x1; cursorPositionY1 = y1
            cursorPositionX2 = x2; cursorPositionY2 = y2
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WonderBeeTheme.materialScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎨 Custom 2D Color Picker Matrix", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WonderBeeTheme.materialScheme.onBackground)
                    Text("Drag inside the spectrum to overlay custom colors over your theme layout.", fontSize = 11.sp, color = Color.Gray)
                }

                if (ThemePreferencesManager.isCustomColorEnabled) {
                    TextButton(
                        onClick = {
                            ThemePreferencesManager.isCustomColorEnabled = false
                            ThemePreferencesManager.saveThemeState(
                                ThemePreferencesManager.currentAppThemeSelection,
                                false, cursorPositionX1, cursorPositionY1, cursorPositionX2, cursorPositionY2
                            )
                        }
                    ) {
                        Text("Reset Custom", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedButton(
                    onClick = { selectingFirstColor = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectingFirstColor) ThemePreferencesManager.customColorStart.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(ThemePreferencesManager.customColorStart))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Color 1", fontSize = 12.sp, color = WonderBeeTheme.materialScheme.onBackground)
                }

                OutlinedButton(
                    onClick = { selectingFirstColor = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!selectingFirstColor) ThemePreferencesManager.customColorEnd.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(ThemePreferencesManager.customColorEnd))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Color 2", fontSize = 12.sp, color = WonderBeeTheme.materialScheme.onBackground)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(selectingFirstColor) {
                        fun processInputCoordinate(offsetX: Float, offsetY: Float) {
                            val percentageX = (offsetX / size.width).coerceIn(0f, 1f)
                            val percentageY = (offsetY / size.height).coerceIn(0f, 1f)
                            val pickedColor = ThemePreferencesManager.getColorFrom2DMatrix(percentageX, percentageY)

                            if (selectingFirstColor) {
                                cursorPositionX1 = percentageX
                                cursorPositionY1 = percentageY
                                ThemePreferencesManager.customColorStart = pickedColor
                            } else {
                                cursorPositionX2 = percentageX
                                cursorPositionY2 = percentageY
                                ThemePreferencesManager.customColorEnd = pickedColor
                            }

                            ThemePreferencesManager.isCustomColorEnabled = true
                            ThemePreferencesManager.saveThemeState(
                                ThemePreferencesManager.currentAppThemeSelection, ThemePreferencesManager.isCustomColorEnabled,
                                cursorPositionX1, cursorPositionY1,
                                cursorPositionX2, cursorPositionY2
                            )
                        }

                        detectDragGestures { change, _ ->
                            change.consume()
                            processInputCoordinate(change.position.x, change.position.y)
                        }
                    }
                    .pointerInput(selectingFirstColor) {
                        fun processInputCoordinate(offsetX: Float, offsetY: Float) {
                            val percentageX = (offsetX / size.width).coerceIn(0f, 1f)
                            val percentageY = (offsetY / size.height).coerceIn(0f, 1f)
                            val pickedColor = ThemePreferencesManager.getColorFrom2DMatrix(percentageX, percentageY)

                            if (selectingFirstColor) {
                                cursorPositionX1 = percentageX
                                cursorPositionY1 = percentageY
                                ThemePreferencesManager.customColorStart = pickedColor
                            } else {
                                cursorPositionX2 = percentageX
                                cursorPositionY2 = percentageY
                                ThemePreferencesManager.customColorEnd = pickedColor
                            }

                            ThemePreferencesManager.isCustomColorEnabled = true
                            ThemePreferencesManager.saveThemeState(
                                ThemePreferencesManager.currentAppThemeSelection, ThemePreferencesManager.isCustomColorEnabled,
                                cursorPositionX1, cursorPositionY1,
                                cursorPositionX2, cursorPositionY2
                            )
                        }

                        detectTapGestures { offset ->
                            processInputCoordinate(offset.x, offset.y)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spectrumBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    )
                    drawRect(brush = spectrumBrush)

                    val whiteToTransparentBrush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Transparent, Color.Transparent)
                    )
                    drawRect(brush = whiteToTransparentBrush)

                    val transparentToBlackBrush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color.Black)
                    )
                    drawRect(brush = transparentToBlackBrush)

                    val cursorRadius = 10.dp.toPx()

                    val center1 = Offset(cursorPositionX1 * size.width, cursorPositionY1 * size.height)
                    drawCircle(color = Color.White, radius = cursorRadius, center = center1, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = cursorRadius - 1.dp.toPx(), center = center1, style = Stroke(width = 1.dp.toPx()))

                    val center2 = Offset(cursorPositionX2 * size.width, cursorPositionY2 * size.height)
                    drawCircle(color = Color.White, radius = cursorRadius, center = center2, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(color = Color.Black.copy(alpha = 0.6f), radius = cursorRadius - 1.dp.toPx(), center = center2, style = Stroke(width = 1.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (ThemePreferencesManager.isCustomColorEnabled) {
                            Brush.horizontalGradient(listOf(ThemePreferencesManager.customColorStart, ThemePreferencesManager.customColorEnd))
                        } else {
                            Brush.horizontalGradient(listOf(WonderBeeTheme.materialScheme.primary, WonderBeeTheme.materialScheme.secondary))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (ThemePreferencesManager.isCustomColorEnabled) "Active Custom Overlay Gradient" else "Preset Theme Gradient (Matrix Inactive)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
