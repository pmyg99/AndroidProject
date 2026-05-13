plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.yolotest"
    compileSdk = 35
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "com.example.yolotest"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        renderscriptTargetApi = 24
        renderscriptSupportModeEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    dependencies {
        // ... 其他依赖
        implementation("org.nanohttpd:nanohttpd:2.3.1")
        // CameraX 核心库
        implementation("androidx.camera:camera-core:1.5.0")
        // Camera2 互操作库
        implementation("androidx.camera:camera-camera2:1.5.0")
        // 生命周期库
        implementation("androidx.camera:camera-lifecycle:1.5.0")
        // 视图库
        implementation("androidx.camera:camera-view:1.5.0")
    }
}