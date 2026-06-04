plugins {
    // This is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    // 1. Add the Google Services dependency alias here
    alias(libs.plugins.googleServices) apply false

    id("com.google.firebase.crashlytics") version "3.0.7" apply false

}
