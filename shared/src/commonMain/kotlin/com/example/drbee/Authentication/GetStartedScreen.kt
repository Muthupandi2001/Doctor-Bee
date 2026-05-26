package com.example.drbee.Authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aravind.composefitnessapp.ui.theme.poppinsFamily
import com.example.drbee.Helper.dynamicColorLottieAnimation
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.bee_doc
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import theme.AppColors.GradientEnd
import theme.AppColors.GradientStart
import theme.AppColors.Grey1


@OptIn(ExperimentalTextApi::class)
@Composable
fun GetStartedScreen(onGetStarted: () -> Unit) {
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = true) {
//        Log.d("LaunchedE:::", "LaunchedEffect::")
        delay(1000)
        showSplashScreen = false
    }


    if (showSplashScreen) {
        splashScreen()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Color.Black)
        )
        {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 20.dp)
                        .scale(1.6f),
                    contentAlignment = Alignment.Center
                ) {

                    dynamicColorLottieAnimation(
                        animationPath = "files/beelooking.json",
                        speed = 2f,
                        modifier = Modifier.size(250.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))


            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        )
        {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 25.dp)
            )
            {

                Text(
                    buildAnnotatedString {

                        withStyle(
                            style = SpanStyle(
                                fontSize = 36.sp,
                                fontFamily = poppinsFamily(),
                                fontWeight = FontWeight.Bold,
                                brush = Brush.horizontalGradient(
                                    listOf(GradientStart, GradientEnd)
                                )
                            )
                        ) {
                            append("FITNESS ")
                        }

                        withStyle(
                            style = SpanStyle(
                                color = Color.White,
                                fontSize = 34.sp,
                                fontFamily = poppinsFamily(),
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append("Doctor")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Everybody Can Train",
                    fontFamily = poppinsFamily(),
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Grey1
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 55.dp, end = 55.dp, bottom = 65.dp, top = 30.dp)
                    .height(65.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            )
            {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(GradientStart, GradientEnd)
                            ),
                            shape = RoundedCornerShape(30.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Get Started",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = poppinsFamily(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }


}


@Composable
fun splashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(Res.drawable.bee_doc),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
