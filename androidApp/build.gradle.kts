import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

}

dependencies {

    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging:24.0.0")

    // Google Auth (for OAuth2 / service account)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("dev.gitlive:firebase-auth:1.13.0")
    implementation("dev.gitlive:firebase-database:1.13.0")

    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ✅ FIX: Add the missing native Android SDK artifact managed by the BoM platform
    implementation("com.google.firebase:firebase-database")


    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.example.drbee"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.drbee"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Add these three lines:
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
