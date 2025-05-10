plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aareno.seen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aareno.seen"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // components
    implementation(libs.appcompat)
    implementation(libs.material)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.workmanager)

    // Glide
    implementation(libs.gson)
    implementation(libs.glide)

    // database
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)

    // Firebase core
    implementation(libs.firebase.core)

    // Firebase Authentication (for Google Sign-In)
    implementation(libs.firebase.auth)

    // Firestore for data storage
    implementation(libs.firebase.firestore)

    // Google Play services for auth
    implementation(libs.play.services.auth)

    // Add Guava
    implementation("com.google.guava:guava:32.1.3-android")

    // test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}