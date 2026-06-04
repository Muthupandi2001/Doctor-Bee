//package com.example.drbee // Double check that this matches commonMain exactly
//
//import com.google.firebase.crashlytics.FirebaseCrashlytics
//import io.github.aakira.napier.Antilog
//import io.github.aakira.napier.LogLevel
//
//class AndroidCrashlyticsAntilog : Antilog() {
//    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
//        // Safe check block wrapper to ensure your app won't crash if Crashlytics fails internally
//        try {
//            val crashlytics = FirebaseCrashlytics.getInstance()
//
//            // Log basic debug messages as custom keys or breadcrumbs
//            message?.let { crashlytics.log("[${priority.name}] $tag: $it") }
//
//            // If an operation throws an exception, forward it explicitly to the dashboard
//            throwable?.let { crashlytics.recordException(it) }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//}
//
//actual fun getCrashlyticsAntilog(): Antilog = AndroidCrashlyticsAntilog()
