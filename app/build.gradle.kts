import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("androidx.room")
}

android {
    namespace = "easy_a.application"
    compileSdk = 34

    defaultConfig {
        applicationId = "easy_a.application"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Get the API key from local.properties
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
        buildConfigField("String", "AI_API_KEY", "\"${localProperties["AI_API_KEY"]}\"")
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
        viewBinding = true
        buildConfig = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Retrofit dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.firebase.auth)
    implementation(libs.google.services)

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:19.2.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("io.noties.markwon:core:4.6.2")

    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation ("com.google.firebase:firebase-bom:32.0.0")
    implementation("com.google.firebase:firebase-messaging")

    implementation(libs.firebase.messaging.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)

    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
