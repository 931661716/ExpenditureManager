plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // This plugin is for Compose compiler, ensure it's here
    id("com.google.gms.google-services") // This must be applied for Firebase
}

android {
    namespace = "com.example.expendituremanager" // Replace with your actual package name
    compileSdk = 35 // Or a newer API level if you prefer

    defaultConfig {
        applicationId = "com.example.expendituremanager" // Replace with your actual package name
        minSdk = 26 // Minimum SDK for Firebase and modern Compose
        targetSdk = 35 // Or a newer API level
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // This should match your Compose BOM's compiler version.
        // For compose-bom:2025.05.01, check the compatible compiler version.
        // You might need to adjust this based on the Android Developers documentation for your BOM.
        // A common practice is to keep this in sync with the BOM.
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Declare the Compose BOM first to manage compatible versions of Compose libraries
    implementation(platform(libs.androidx.compose.bom))

    // Core Compose UI, Material3, and Icons (versions managed by the BOM)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended) // Explicitly include extended icons

    // Activity and Navigation for Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime)


    // Compose Foundation (for FlowRow, its version is managed by BOM)
    implementation(libs.androidx.compose.foundation.layout)

    // Firebase Libraries (versions managed by Firebase BOM)
    implementation(platform(libs.firebase.bom)) // Declare the Firebase BOM
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Compose Charts
    //implementation(libs.compose.charts)
    //implementation(libs.compose.charts.bar)
    // If you add other chart types later, uncomment them:
    // implementation(libs.compose.charts.pie)

    // Gson
    implementation(libs.gson) // Add this line for Gson


    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Debugging and Preview Tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}