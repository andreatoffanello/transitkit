import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wire)
    id("com.github.triplet.play")
}

// google-services is applied at the bottom of the file conditionally —
// only when the operator-specific google-services.json is present in
// app/. Missing file is non-fatal so operators without push enabled
// can still build (PushNotificationManager will gracefully no-op).
val googleServicesJson = file("google-services.json")

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> localProps.load(stream) }
}

android {
    namespace = "com.transitkit.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.transitkit.appalcart"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.2.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "MAPBOX_ACCESS_TOKEN",
            "\"${localProps.getProperty("MAPBOX_ACCESS_TOKEN", "")}\""
        )
        buildConfigField(
            "String",
            "ROUTING_API_KEY",
            "\"${localProps.getProperty("ROUTING_API_KEY", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("KEYSTORE_FILE", ""))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = localProps.getProperty("KEY_ALIAS", "")
            keyPassword = localProps.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
        buildConfig = true
    }
}

wire {
    kotlin {}
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Mapbox — unico renderer mappe (principale + dettaglio)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.compose)
    implementation(libs.play.services.location)

    implementation(libs.datastore.preferences)
    implementation(libs.wire.runtime)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.accompanist.permissions)

    // Firebase Cloud Messaging — wired regardless of whether
    // google-services.json is present so the code compiles; the
    // PushNotificationManager guards runtime against missing config.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
}

// Apply the google-services plugin only when the operator-specific
// json is in place. This keeps the build green for operators not yet
// onboarded to Firebase.
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

play {
    val credPath = providers.gradleProperty("playCredentials")
        .orElse("${System.getProperty("user.home")}/.config/google-play/publisher-key.json")
    serviceAccountCredentials.set(file(credPath.get()))
    track.set(providers.gradleProperty("playTrack").orElse("internal"))
    defaultToAppBundles.set(true)
}
