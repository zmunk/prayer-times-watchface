/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Use the locally-defined validator to demonstrate validation on-build.
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.example.android.wearable.wear.complications"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.complications"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField(
            "String",
            "API_URL",
            "\"${System.getenv("API_URL")}\"",
        )
    }

    buildTypes {
        release {
            // TODO:Add your signingConfig here to build release builds
            isMinifyEnabled = true
            // Ensure shrink resources is false, to avoid potential for them
            // being removed.
            isShrinkResources = false

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.wear.watchface.complications.data.source.ktx)
    implementation("androidx.wear.watchface:watchface-editor:1.2.0-alpha03")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}