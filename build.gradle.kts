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
    // Defines the Android Application plugin at the project level
    // 'apply false' means this plugin is not applied to the project itself,
    // but can be applied to specific submodules
    // Helps manage plugin versions consistently across the project
    alias(libs.plugins.android.application) apply false

    // Defines the Kotlin Android plugin at the project level
    // 'apply false' allows submodules to apply this plugin 
    // with a consistent version defined in the version catalog
    alias(libs.plugins.kotlin.android) apply false
}
