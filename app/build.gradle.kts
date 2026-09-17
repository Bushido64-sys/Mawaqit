import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

// ── Build stamp ──────────────────────────────────────────────────────────────
// Every APK carries the exact commit it was built from, in three places:
//   1. versionName            → "1.0.0-<sha>"
//   2. the APK file name      → "Mawaqit-1.0.0-<sha>-debug.apk"
//   3. the app's first screen → "Build: <sha>"
// This is read automatically at build time — no placeholders to update by hand.
val gitShortSha: String = try {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = out
        isIgnoreExitValue = false
    }
    out.toString("UTF-8").trim()
} catch (e: Exception) {
    "dev" // building outside a git checkout (e.g. a plain zip download)
}

android {
    namespace   = "com.mawaqit.app"
    compileSdk  = 34

    defaultConfig {
        applicationId   = "com.mawaqit.app"
        minSdk          = 26
        targetSdk       = 34
        versionCode     = 1
        versionName     = "1.0.0-$gitShortSha"   // e.g. 1.0.0-e987c71

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Exposed to Kotlin as BuildConfig.GIT_SHA (shown on the app screen)
        buildConfigField("String", "GIT_SHA", "\"$gitShortSha\"")
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose      = true
        buildConfig  = true // needed for BuildConfig.GIT_SHA
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // NOTE: the APK file name is not changed inside Gradle on purpose —
    // that would require AGP internal APIs (fragile). codespaces/build.sh
    // renames the finished APK to Mawaqit-1.0.0-<sha>-debug.apk instead.
}

dependencies {

    // ── Compose BOM (pins all Compose library versions together) ──
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Core Android ──────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0") // per-app language switching (PHASE_8)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Navigation ────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── ViewModel ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // ── Hilt (Dependency Injection) ───────────────────────────────
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Room (Local Database) ─────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Retrofit + Gson (API calls) ───────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Coroutines ────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ── Location (for GPS prayer times + Qibla) ───────────────────
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ── WorkManager (for alarm rescheduling after boot) ───────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ── Glance (Home screen widget) ───────────────────────────────
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // ── DataStore (modern SharedPreferences replacement) ──────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Downloadable Fonts (Inter + Noto Naskh Arabic) ────────────
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")

    // ── Testing ───────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
