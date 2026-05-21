import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    applyDefaultHierarchyTemplate()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    js {
        browser()
    }

    androidLibrary {
        namespace = "com.example.drbee.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.1.0"))
        }

        commonMain.dependencies {
            implementation("dev.gitlive:firebase-auth:2.4.0")
            implementation("dev.gitlive:firebase-database:2.4.0")

            // CLEANED: Removed the deprecated ui-backhandler line to prevent runtime crashes!
            implementation("org.jetbrains.compose.ui:ui:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.11.0")
            implementation("io.github.alexzhirkevich:compottie:2.1.0")
            implementation("io.github.alexzhirkevich:compottie-resources:2.1.0")
            implementation(compose.materialIconsExtended)
            implementation(libs.napier)

            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")


            // Modern KMP Navigation Event Handling (Keep this one)
            implementation("org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

// CLEANED: Removed the broad global freeCompilerArgs.add opt-in block from here!

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
