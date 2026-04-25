import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

// Optional local/CI signing. If present, release variants will be signed automatically.
val llSigningStoreFile = signingValue("LL_SIGNING_STORE_FILE")
val llSigningStorePassword = signingValue("LL_SIGNING_STORE_PASSWORD")
val llSigningKeyAlias = signingValue("LL_SIGNING_KEY_ALIAS")
val llSigningKeyPassword = signingValue("LL_SIGNING_KEY_PASSWORD")
val llSigningStoreType = signingValue("LL_SIGNING_STORE_TYPE") // e.g. "PKCS12" or "JKS"
val llHasSigning = !llSigningStoreFile.isNullOrBlank() &&
    !llSigningStorePassword.isNullOrBlank() &&
    !llSigningKeyAlias.isNullOrBlank() &&
    !llSigningKeyPassword.isNullOrBlank()

android {
    namespace = "com.lunarlog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lunarlog"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.7.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (llHasSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(llSigningStoreFile!!)
                if (!llSigningStoreType.isNullOrBlank()) {
                    storeType = llSigningStoreType
                }
                storePassword = llSigningStorePassword
                keyAlias = llSigningKeyAlias
                keyPassword = llSigningKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (llHasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        if (project.findProperty("enableComposeCompilerMetrics") == "true") {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.layout.buildDirectory.get().asFile.absolutePath + "/compose_metrics",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + project.layout.buildDirectory.get().asFile.absolutePath + "/compose_metrics"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "false")
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"play\"")
        }
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "true")
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"github\"")
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt (Dagger)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Charts (Vico)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Phase 6: Privacy & Polish
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.androidx.core.splashscreen)

    // Widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
