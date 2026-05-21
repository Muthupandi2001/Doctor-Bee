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

    // ✅ FIX: Re-add the standard JavaScript target so your ':webApp' module can resolve it
    js {
        browser()
    }

    androidLibrary {
        namespace = "com.example.drbee.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // ❌ REMOVE THIS BLOCK COMPLETELY (It causes script compilation errors):
        // compilerOptions {
        //     jvmTarget = JvmTarget.JVM_11
        // }

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

            // Add the Firebase BoM platform directly to the shared android target
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.1.0"))
        }

        commonMain.dependencies {
            // GitLive Firebase handles Android, iOS, and standard JS out of the box!
//            implementation("dev.gitlive:firebase-auth:1.13.0")
//
////            implementation("dev.gitlive:firebase-firestore:1.13.0")
//            implementation("dev.gitlive:firebase-database:1.13.0")

            implementation("dev.gitlive:firebase-auth:2.4.0")
            implementation("dev.gitlive:firebase-database:2.4.0")

            implementation("org.jetbrains.compose.ui:ui-backhandler:1.9.1")
            implementation("org.jetbrains.compose.ui:ui:1.7.3")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

            // Standard Shared Compose Multiplatform UI Dependencies
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Third-party Navigation & Animation libraries
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
            implementation("io.github.alexzhirkevich:compottie:2.1.0")
            implementation("io.github.alexzhirkevich:compottie-resources:2.1.0")
            implementation(compose.materialIconsExtended)
            implementation(libs.napier)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // ✅ FIX: Declare the web-specific main source set dependency block
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
