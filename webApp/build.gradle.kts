// 1. Explicitly import the YarnRootExtension type to fix compilation errors
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport

// 2. Safely apply settings to the root project using the correct extension type accessor
rootProject.extensions.findByType(YarnRootExtension::class.java)?.apply {
    yarnLockMismatchReport = YarnLockMismatchReport.NONE
    yarnLockAutoReplace = true
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // ✅ Keep ONLY standard JavaScript target active
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // ✅ Cleaned up the duplicate project import statement
            implementation(project(":shared"))

            implementation(libs.compose.ui)

            // ✅ Declare the matching GitLive Firebase library explicitly here
            implementation("dev.gitlive:firebase-auth:1.13.0")

            implementation("dev.gitlive:firebase-database:1.13.0")

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        }
    }
}
