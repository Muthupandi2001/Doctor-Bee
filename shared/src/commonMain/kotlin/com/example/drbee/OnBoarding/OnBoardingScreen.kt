package com.example.drbee.OnBoarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aravind.composefitnessapp.ui.theme.poppinsFamily
import com.example.drbee.Helper.dynamicColorLottieAnimation
import drbee.shared.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import theme.AppColors.Pink1
import theme.AppStrings.TRACK_GOAL_TITLE


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnBoardingScreen(onBoardingFinished: () -> Unit) {
    val onBoardingItemList = OnBoardingItem.getData()
    val scope = rememberCoroutineScope()
    val pageState = rememberPagerState {
        onBoardingItemList.size
    }

    var showAnimation by remember { mutableStateOf(true) }


    LaunchedEffect(showAnimation) {

        if (showAnimation) {

            delay(800)

            showAnimation = false
        }
    }



    Box(contentAlignment = Alignment.Center) {



        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)) {


            HorizontalPager(
                state = pageState,
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxSize()
                    .weight(0.8f)
            ) { page ->
                OnBoardingItem(item = onBoardingItemList[page])
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.2f)
                    .padding(bottom = 30.dp, end = 25.dp),
                contentAlignment = Alignment.TopEnd
            ) {

//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 20.dp)
//                    .scale(2.6f),
//                contentAlignment = Alignment.Center
//            )
//            {
//
//                dynamicColorLottieAnimation(
//                    animationResId = R.raw.realbee,
//                    color = colorResource(R.color.trnsparnt),
//                    dataReceived = true,
//                    speed = 0.8f,
//                    modifier = Modifier.size(250.dp)
//                )
//            }

                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                )
                {
                    Image(
                        painter = painterResource(Res.drawable.ic_next_button_rounded),
                        contentDescription = "Next button",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (pageState.currentPage + 1 < onBoardingItemList.size) scope.launch {
                                    pageState.animateScrollToPage(pageState.currentPage + 1)
                                } else if (pageState.currentPage == 3) {
                                    onBoardingFinished()
                                }
                            })
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(2.6f),
            contentAlignment = Alignment.Center
        )
        {
            dynamicColorLottieAnimation(
                animationPath = "files/realbee.json",
                speed = 1.0f,
                modifier = Modifier.size(250.dp)
            )
        }

    }

//    if (showAnimation) {
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .scale(4f),
//            contentAlignment = Alignment.Center
//        ) {
//
//            dynamicColorLottieAnimation(
//                animationResId = R.raw.groupbee,
//                color = colorResource(R.color.trnsparnt),
//                dataReceived = true,
//                speed = 4f,
//                modifier = Modifier.size(300.dp)
//            )
//        }
//    }
}


@Composable
fun OnBoardingItem(item: OnBoardingItem) {
    Column(verticalArrangement = Arrangement.Top) {
        Image(
            painter = painterResource(item.image),
            contentDescription = TRACK_GOAL_TITLE,
            modifier = Modifier
                .fillMaxSize()
                .weight(0.7f),
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 30.dp)
                .weight(0.3f)
        ) {
            Text(
                text = item.title, color = Pink1, fontSize = 24.sp,
                fontFamily = poppinsFamily(), fontWeight = FontWeight.Bold
            )

            Text(
                text = item.desc,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = poppinsFamily(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
