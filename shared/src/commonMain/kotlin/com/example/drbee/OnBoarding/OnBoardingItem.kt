package com.example.drbee.OnBoarding

// Core JetBrains multiplatform resource types
import drbee.shared.generated.resources.Res
import drbee.shared.generated.resources.ic_eat_well
import drbee.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

// Generated local project resource hooks

import theme.AppStrings.EAT_WELL_DESC
import theme.AppStrings.EAT_WELL_TITLE
import theme.AppStrings.GET_BURN_DESC
import theme.AppStrings.GET_BURN_TITLE
import theme.AppStrings.IMPROVE_SLEEP_DESC
import theme.AppStrings.IMPROVE_SLEEP_TITLE
import theme.AppStrings.TRACK_GOAL_DESC
import theme.AppStrings.TRACK_GOAL_TITLE


class OnBoardingItem(
    val image: DrawableResource, // ✅ Changed from Int to DrawableResource
    val title: String,   // ✅ Changed from Int to StringResource
    val desc: String     // ✅ Changed from Int to StringResource
) {
    companion object {
        fun getData(): List<OnBoardingItem> {
            return listOf(
                OnBoardingItem(
                    Res.drawable.ic_track_goal,
                    TRACK_GOAL_TITLE,
                    TRACK_GOAL_DESC
                ),
                OnBoardingItem(
                    Res.drawable.ic_get_burn,
                    GET_BURN_TITLE,
                    GET_BURN_DESC
                ),
                OnBoardingItem(
                    Res.drawable.ic_eat_well,
                    EAT_WELL_TITLE,
                    EAT_WELL_DESC
                ),
                OnBoardingItem(
                    Res.drawable.ic_improve_sleep,
                    IMPROVE_SLEEP_TITLE,
                    IMPROVE_SLEEP_DESC
                )
            )
        }
    }
}