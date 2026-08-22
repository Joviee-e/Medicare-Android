plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.medicare"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.medicare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load Geoapify API key at build time without hardcoding or committing it
        val envFile = project.rootProject.file("../Medicare-Backend/.env")
        val localEnvFile = project.rootProject.file(".env")
        val targetEnvFile = if (localEnvFile.exists()) localEnvFile else envFile
        var geoapifyKey = ""
        if (targetEnvFile.exists()) {
            targetEnvFile.forEachLine { line ->
                if (line.startsWith("GEOAPIFY_API_KEY=")) {
                    geoapifyKey = line.substringAfter("GEOAPIFY_API_KEY=").trim()
                }
            }
        }
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyKey\"")
    }

    buildFeatures {
        buildConfig = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation(libs.maplibre.sdk)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}