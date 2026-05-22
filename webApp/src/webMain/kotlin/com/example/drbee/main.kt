package com.example.drbee

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    val webOptions = FirebaseOptions(
        applicationId = "1:444402775117:web:82608e86b7a6afde1b1804",
        apiKey        = "AIzaSyBzHwoHb9KhxPDvdPfkOtI8BbjFprgYgXk",
        projectId     = "doctor-bee-2d622",
        storageBucket = "doctor-bee-2d622.firebasestorage.app",
        databaseUrl   = "https://doctor-bee-2d622-default-rtdb.firebaseio.com"
    )

    Firebase.initialize(options = webOptions)

    // ✅ Parse deep link params from the browser URL query string
    val urlParams   = URLSearchParams(window.location.search)
    val screen      = urlParams.get("screen")
    val referrerId  = urlParams.get("referrerId")

    val deepLinkParams = if (!screen.isNullOrBlank() || !referrerId.isNullOrBlank()) {
        DeepLinkParams(screen = screen, referrerId = referrerId)
    } else {
        null
    }

    ComposeViewport(document.body!!) {
        App(
            deepLinkParams   = deepLinkParams,        // ✅ parsed from browser URL
            onShareRequested = { userId ->
                shareReferralLinkWeb(userId)          // ✅ no 'this' needed on web
            }
        )
    }
}

fun shareReferralLinkWeb(userId: String) {
    val baseUrl          = "https://creative-bunny-e05f99.netlify.app"
    val fullReferralUrl  = "$baseUrl?screen=referral&referrerId=$userId"
    val shareText        = "Hey! Join DrBee using my invitation link: $fullReferralUrl"
    val shareTitle       = "Join DrBee App!"

    if (js("typeof navigator.share !== 'undefined'") as Boolean) {
        // ✅ Pass variables safely into JS — don't reference Kotlin vars inside js() string
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

// ✅ KMP-safe URLSearchParams wrapper — no extra library needed
external class URLSearchParams(init: String) {
    fun get(name: String): String?
}