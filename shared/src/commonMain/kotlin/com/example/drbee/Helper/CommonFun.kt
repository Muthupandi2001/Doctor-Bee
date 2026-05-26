package com.example.drbee.Helper
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.font_bold
import drbee.shared.generated.resources.font_extrabold
import drbee.shared.generated.resources.font_regular
import drbee.shared.generated.resources.font_semibold
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

import io.github.alexzhirkevich.compottie.*


import org.jetbrains.compose.resources.Font
import kotlin.math.cos
import kotlin.math.sin

/* ---------------- Back ---------------- */

@Composable
fun BackPressed(navController: NavController) {
    navController.popBackStack()
}

/* ---------------- Fonts ---------------- */

@Composable
fun fontRegular() = FontFamily(
    Font(
        resource = Res.font.font_regular,
        weight = FontWeight.Normal
    )
)

@Composable
fun fontBold() = FontFamily(
    Font(
        resource = Res.font.font_bold,
        weight = FontWeight.Bold
    )
)

@Composable
fun fontSemibold() = FontFamily(
    Font(
        resource = Res.font.font_semibold,
        weight = FontWeight.SemiBold
    )
)

@Composable
fun fontExtrabold() = FontFamily(
    Font(
        resource = Res.font.font_extrabold,
        weight = FontWeight.ExtraBold
    )
)
/* ---------------- Lottie ---------------- */
@Composable
fun dynamicColorLottieAnimation(
    animationPath: String,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    iterations: Int = Compottie.IterateForever
) {

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(animationPath).decodeToString()
        )
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed
    )

    val painter = rememberLottiePainter(
        composition = composition,
        progress = { progress }
    )

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
/* ---------------- Colors ---------------- */

val BeeWarmIvory = Color(0xFFFFFBEA)
val BeeDarkNavy = Color(0xFF0F1A34)
val BeeAmberGold = Color(0xFFD09200)
val BeeBrightYellow = Color(0xFFFFC107)
val BeeMutedGold = Color(0xFFE6C687)


val HexagonShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val radius = width / 2
    val centerX = width / 2
    val centerY = height / 2

    moveTo(centerX + radius * cos(0.0).toFloat(), centerY + radius * sin(0.0).toFloat())
    for (i in 1..5) {
        val angle = 0.523599 * 2 * i // 60 degrees in radians
        lineTo(centerX + radius * cos(angle).toFloat(), centerY + radius * sin(angle).toFloat())
    }
    close()
}

object Logger {

    fun init() {
        Napier.base(DebugAntilog())
    }
}


 val BeeBackground = Color(0xFFFFFBF0)
 val BeeAmber = Color(0xFFFFB300)
 val BeeAmberLight = Color(0xFFFFE082)
 val BeeBrown = Color(0xFF3E2723)
val BeeCard = Color(0xFFFFFFFF)

