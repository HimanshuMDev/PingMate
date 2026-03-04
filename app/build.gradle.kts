plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.app.pingmate"
    compileSdk = 36

    // Build as Android App Bundle (./gradlew bundleRelease) for smaller downloads:
    // Play Store serves per-ABI APKs so users get ~25–50% smaller installs.
    defaultConfig {
        applicationId = "com.app.pingmate"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Gemini API key is set by the user in Settings – no hardcoded key in the app
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material Components (needed for XML theme resolution)
    //noinspection GradleDependency
    implementation("com.google.android.material:material:1.12.0")

    // Material Icons Extended (added inline — not in catalog yet)
    //noinspection UseTomlInstead
    implementation("androidx.compose.material:material-icons-extended")

    // Accompanist Drawable Painter
    //noinspection NewerVersionAvailable
    implementation("com.google.accompanist:accompanist-drawablepainter:0.34.0")

    // Navigation
    implementation(libs.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.compose.foundation.layout)
    ksp(libs.room.compiler)

    // Koin DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // WorkManager
    implementation(libs.workmanager.ktx)

    // Lifecycle ViewModel Compose
    implementation(libs.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Splash Screen
    implementation(libs.splashscreen)

    // Security Crypto
    implementation(libs.security.crypto)

    // Coroutines
    implementation(libs.coroutines.android)

    // Glance Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Lottie Compose for AI Voice Animation
    //noinspection NewerVersionAvailable
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // SavedState (for Overlay ComposeView lifecycle)
    implementation("androidx.savedstate:savedstate:1.2.1")

    // Paging 3
    implementation("androidx.paging:paging-compose:3.2.1")
    implementation("androidx.room:room-paging:2.6.1")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}