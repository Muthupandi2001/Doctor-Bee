import SwiftUI
import Firebase
import Shared

@main
struct iOSApp: App {
    init() {
        // 1. Initialize Native iOS Firebase
        FirebaseApp.configure()

        // 2. Wire up the KMP Crashlytics Bridge safely in native Swift
        KmpCrashReporter.shared.iosLogger = { priority, tag, message, errorMsg in
            let crashlytics = Crashlytics.crashlytics()

            // Log the breadcrumb
            crashlytics.log("[\(priority)] \(tag): \(message)")

            // Record non-fatal error if one occurred
            if let errorMsg = errorMsg {
                let error = NSError(
                    domain: "com.example.drbee.kmp",
                    code: 0,
                    userInfo: [NSLocalizedDescriptionKey: errorMsg]
                )
                crashlytics.record(error: error)
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}