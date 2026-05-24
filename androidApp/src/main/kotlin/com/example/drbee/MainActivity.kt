package com.example.drbee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

// 1. DEFINE THIS DATA CLASS (It was missing in your snippet)
data class DeepLinkParams(
    val screen: String? = null,
    val referrerId: String? = null
)

class MainActivity : ComponentActivity() {

    var deepLinkParams by mutableStateOf<DeepLinkParams?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        processIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = android.graphics.Color.BLACK
// Source - https://stackoverflow.com/a/79870375
// Posted by Ajay Satpati
// Retrieved 2026-05-24, License - CC BY-SA 4.0

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                "#000000".toColorInt()
            ),
            navigationBarStyle = SystemBarStyle.dark(
                "#000000".toColorInt()
            )
        )



        setContent {
            App(
                deepLinkParams = deepLinkParams,
                onShareRequested = { userId ->
                    shareReferralLink(this, userId)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        val data: Uri? = intent?.data

        if (data != null) {
            when {
                // Handles: drbee://open?screen=referral&referrerId=ABC123
                data.scheme == "drbee" && data.host == "open" -> {
                    val screen     = data.getQueryParameter("screen")
                    val referrerId = data.getQueryParameter("referrerId")

                    Log.d("DrBeeDeepLink", "screen=$screen, referrerId=$referrerId")
                    deepLinkParams = DeepLinkParams(screen, referrerId)

                    if (screen == "referral" && referrerId != null) {
                        Toast.makeText(
                            this,
                            "Welcome! Referred by: $referrerId",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                else -> deepLinkParams = null
            }
        } else {
            deepLinkParams = null
        }
    }

    private fun shareReferralLink(context: Context, userId: String) {
        // ✅ Replace with the NEW Netlify URL from Step 2
//        val baseUrl = "https://creative-bunny-e05f99.netlify.app"

        val baseUrl = "https://merry-parfait-8fe34a.netlify.app/"
        val fullUrl = "$baseUrl?screen=referral&referrerId=$userId"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join DrBee App!")
            putExtra(Intent.EXTRA_TEXT, "Hey! Join DrBee: $fullUrl")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Link Via"))
    }


}