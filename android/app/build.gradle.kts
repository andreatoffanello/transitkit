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

// White-label: applicationId e app_name derivano dallo STESSO config.json che
// l'app carica a runtime — build-android.sh lo copia qui da
// shared/operators/<op>/ prima di invocare Gradle. Leggerlo (invece di
// ricevere un -POPERATOR_ID) rende impossibile per costruzione che il package
// installato e il config bundlato divergano: sono la stessa fonte.
// L'OS legge il nome sotto l'icona prima che il nostro codice giri, quindi
// dev'essere cotto qui e non risolto a runtime come il resto delle stringhe.
val operatorConfig: Map<*, *> = file("src/main/assets/config.json").let { f ->
    require(f.exists()) { "config.json mancante in ${f.path} — esegui scripts/build-android.sh <operator_id>" }
    @Suppress("UNCHECKED_CAST")
    groovy.json.JsonSlurper().parse(f) as Map<*, *>
}
val operatorId = requireNotNull(operatorConfig["id"] as? String) { "campo 'id' mancante in config.json" }
val operatorName = requireNotNull(operatorConfig["name"] as? String) { "campo 'name' mancante in config.json" }
// brandName è opzionale: fallback su name, stessa regola di iOS e del client.
val brandName = (operatorConfig["brandName"] as? String) ?: operatorName

android {
    namespace = "com.transitkit.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.transitkit.$operatorId"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.2.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Etichetta sotto l'icona. Definita qui e NON in strings.xml: era
        // duplicata in values/, values-it/ e values-es/ — un nome proprio non
        // si localizza, e tre copie sono tre occasioni di divergere.
        resValue("string", "app_name", brandName)

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
