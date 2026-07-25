import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// ── OTA release signing (Critical Fix #1: consistent signature = no uninstall) ──
// Credentials live in a git-ignored root `keystore.properties`. Every release must
// be signed with the SAME key, otherwise Android refuses to update in place.
// See the "How to ship a new version" checklist in the repo README.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) FileInputStream(keystorePropertiesFile).use { load(it) }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

// ── App version — the single place to bump on every release ──
// Semantic versioning: MAJOR.MINOR.PATCH. See defaultConfig for how these
// become versionName ("2.1.0") and versionCode (20100).
val versionMajor = 2
val versionMinor = 1
val versionPatch = 1

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    // ⚠️ applicationId must NEVER change once shipped. Changing it makes Android
    //    treat the new APK as a different app (no in-place update; side-by-side install).
    applicationId = "com.aistudio.smartworker.kwzl"
    // java.time is used throughout the data layer; API 26+ ships it natively.
    minSdk = 26
    targetSdk = 36
    // ── Semantic version (MAJOR.MINOR.PATCH) ──────────────────────────────────
    // Bump ONE of these on every release:
    //   • PATCH → bug fixes / small tweaks      (2.1.0 → 2.1.1)
    //   • MINOR → new features, backwards-compatible (2.1.1 → 2.2.0)
    //   • MAJOR → breaking changes / redesigns   (2.2.0 → 3.0.0)
    // versionName is derived as "MAJOR.MINOR.PATCH"; versionCode is derived as a
    // single strictly-increasing Int the OTA updater and Android compare against.
    // Keep MINOR and PATCH below 100 so the ordering never collides.
    versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
    versionName = "$versionMajor.$versionMinor.$versionPatch"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Signing is intentionally resilient so `assembleDebug` and `assembleRelease`
  // both produce an installable APK on a fresh clone with zero manual setup.
  // Priority for the "release" key:
  //  1. root `keystore.properties` (the OTA production key — see Critical Fix #1),
  //  2. env-var upload key (KEYSTORE_PATH / STORE_PASSWORD / KEY_PASSWORD, for CI),
  //  3. fall back to debug signing so the build never fails on a keyless machine.
  val uploadKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  val hasUploadKey = uploadKeystore.exists() &&
    !System.getenv("STORE_PASSWORD").isNullOrEmpty() &&
    !System.getenv("KEY_PASSWORD").isNullOrEmpty()
  val localDebugKeystore = file("${rootDir}/debug.keystore")
  val hasReleaseSigning = hasReleaseKeystore || hasUploadKey

  signingConfigs {
    when {
      // Preferred: production OTA key from keystore.properties.
      hasReleaseKeystore -> create("release") {
        storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      }
      // CI fallback: upload key via environment variables.
      hasUploadKey -> create("release") {
        storeFile = uploadKeystore
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    if (localDebugKeystore.exists()) {
      create("debugConfig") {
        storeFile = localDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Critical Fix #1: release is signed with the real "release" key when available.
      signingConfig = if (hasReleaseSigning) {
        signingConfigs.getByName("release")
      } else {
        getByName("debug").signingConfig
      }
    }
    debug {
      // Critical Fix #1: sign debug with the SAME "release" key when it exists, so
      // debug ↔ release installs on a developer device don't require an uninstall.
      signingConfig = when {
        hasReleaseSigning -> signingConfigs.getByName("release")
        localDebugKeystore.exists() -> signingConfigs.getByName("debugConfig")
        else -> getByName("debug").signingConfig // AGP's auto-generated debug keystore
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.zxing.core)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json) // OTA update manifest parsing
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
