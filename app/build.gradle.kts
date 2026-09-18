plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.boardnotes"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.boardnotes"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Room - local source of truth (DR-15)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // WorkManager - deferred, constraint-aware sync (DR-16)
    implementation(libs.work.runtime)

    // ViewModel + LiveData (DR-08)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // CameraX (Phase 2)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ML Kit on-device OCR (DR-17)
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation(libs.exifinterface)
}