package com.example.drbee

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.drbee.NavHost.DeepLinkParams
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    val webOptions = FirebaseOptions(
        applicationId = "1:444402775117:web:82608e86b7a6afde1b1804",
        apiKey        = "AIzaSyAoWD1PhhfBmZSPXvizEGndJtcRf1dtu_A",
        projectId     = "doctor-bee-2d622",
        storageBucket = "doctor-bee-2d622.firebasestorage.app",
        databaseUrl   = "https://doctor-bee-2d622-default-rtdb.firebaseio.com"
    )

    Firebase.initialize(options = webOptions)

    // Parse deep link params from browser URL
    val urlParams  = URLSearchParams(window.location.search)
    val screen     = urlParams.get("screen")
    val referrerId = urlParams.get("referrerId")

    val deepLinkParams = if (!screen.isNullOrBlank() || !referrerId.isNullOrBlank()) {
        DeepLinkParams(screen = screen, referrerId = referrerId)
    } else {
        null
    }

    ComposeViewport(document.body!!) {
        App(
            deepLinkParams = deepLinkParams,
            onUserLoggedIn = { uid ->
                console.log("Web login: uid=$uid (FCM web push not configured)")
            }
        )
    }
}

fun shareReferralLinkWeb(userId: String) {
    val baseUrl         = "https://creative-bunny-e05f99.netlify.app"
    val fullReferralUrl = "$baseUrl?screen=referral&referrerId=$userId"
    val shareText       = "Hey! Join DrBee using my invitation link: $fullReferralUrl"
    val shareTitle      = "Join DrBee App!"

    if (js("typeof navigator.share !== 'undefined'") as Boolean) {
        val shareData = js("({})")
        shareData.title = shareTitle
        shareData.text  = shareText
        shareData.url   = fullReferralUrl
        window.navigator.asDynamic().share(shareData)
    } else {
        window.navigator.clipboard.writeText(fullReferralUrl)
        window.alert("Link copied! Share it with your friends.")
    }
}

// KMP-safe URLSearchParams wrapper
external class URLSearchParams(init: String) {
    fun get(name: String): String?
}