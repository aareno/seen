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
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)
    val room_version = "2.7.1"

    // components
    implementation(libs.appcompat)
    implementation(libs.material)

    // OkHttp
    implementation(libs.okhttp)
    // Glide
    implementation(libs.gson)
    implementation(libs.glide)
    annotationProcessor(libs.room.compiler)

    // database

    // test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}