plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.app_market_researcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.app_market_researcher"
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
    kotlinOptions {
        jvmTarget = "11"
    }

    // Add this packagingOptions block to resolve duplicate files

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
//    implementation(libs.litert)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // For networking (Retrofit)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // For plotting (MPAndroidChart)
    implementation(libs.mpandroidchart)
    implementation("org.tensorflow:tensorflow-lite:2.12.0") // or newer
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") // or newer
//    implementation(libs.tensorflow.lite)
//    implementation(libs.tensorflow.lite.support)
//    implementation(libs.litert) {
//        exclude(group = "org.tensorflow", module = "tensorflow-lite")
//    }
}