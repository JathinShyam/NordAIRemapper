---
name: compose-modernize-build
description: "Modernize an Android project's Gradle build: bump to Kotlin 2.x with kotlin-compose plugin (replace kotlinCompilerExtensionVersion), migrate kapt to KSP, introduce Version Catalogs (libs.versions.toml), use the Compose BOM, switch to Material 3."
---

# Modernize Android Build

## When to Use

When inheriting an Android project on Kotlin 1.9 / Compose Compiler 1.5.x with manual version pinning, and bringing it forward to Kotlin 2.x with `kotlin-compose` plugin + Version Catalogs.

## Instructions

Apply in order. Sync Gradle and build the project after each step.

### Step 1: Create `libs.versions.toml`

`gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.20"
agp = "9.1.1"
composeBom = "2026.04.01"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
room = "2.7.0"
ksp = "2.3.20-1.0.28"
coroutines = "1.8.1"
serializationJson = "1.7.3"
lifecycle = "2.9.0"
navigation = "2.8.5"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### Step 2: Update the root build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### Step 3: Module build.gradle.kts - replace composeOptions with kotlin-compose plugin

```kotlin
// BEFORE
android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

// AFTER - remove the composeOptions block entirely
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)    // applies Compose Compiler as a Kotlin plugin
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 24
        targetSdk = 36
    }
    buildFeatures {
        compose = true
    }
    // no composeOptions block needed
}
```

### Step 4: kapt to KSP

```kotlin
// BEFORE
plugins {
    id("kotlin-kapt")
}
dependencies {
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    kapt("androidx.room:room-compiler:2.6.1")
}

// AFTER
plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
}
```

Remove any `kapt { correctErrorTypes = true }` blocks and the `kotlin-kapt` plugin.

### Step 5: Compose BOM for version alignment

```kotlin
dependencies {
    val bom = platform(libs.androidx.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

Remove individual version pins on `androidx.compose.*` artifacts - the BOM provides them.

### Step 6: Material 2 to Material 3

```bash
find . -name '*.kt' -print0 | xargs -0 sed -i.bak 's/androidx\.compose\.material\./androidx.compose.material3./g'
find . -name '*.kt.bak' -delete
```

Verify the swap compiles. Some component names differ (`TopAppBar` API changed, `IconButton` parameters renamed). Adjust per the Material 3 docs.

In dependencies, swap:

```kotlin
// BEFORE
implementation("androidx.compose.material:material:...")

// AFTER
implementation(libs.androidx.compose.material3)
```

### Step 7: targetSdk / compileSdk bump

```kotlin
android {
    compileSdk = 36
    defaultConfig {
        minSdk = 24       // adjust to your floor
        targetSdk = 36
    }
}
```

Required by the Play Store from August 2026 onward.

### Step 8: Verify

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest    # if devices available
```

Sync the IDE, confirm builds, run tests.

## Anti-patterns to avoid

- Do not pin `composeCompiler` separately after applying `kotlin-compose` - the plugin owns the version.
- Do not mix `kapt` and `ksp` for the same annotation processor - pick one (ksp).
- Do not leave version literals in module `build.gradle.kts` after introducing the catalog. The whole point is single-source-of-truth.
- Do not migrate Material 2 to Material 3 module-by-module if multiple modules import the same screen - the runtime will pick one and components will look subtly broken.
