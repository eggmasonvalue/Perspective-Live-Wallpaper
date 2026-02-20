plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.timehorizons.wallpaper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.timehorizons.wallpaper"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file(System.getenv("KEYSTORE_FILE") ?: "${rootDir}/keystore.jks")
            storeFile = if (keystoreFile.exists()) keystoreFile else null
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEY_ALIAS") ?: "key0"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            // Fallback to debug signing if release keystore is missing (common in CI for non-release builds or open source)
            // Ideally, production builds should fail if secrets are missing, but we allow debug signing for development convenience.
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    testImplementation("junit:junit:4.13.2")
}
