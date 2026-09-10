import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

val envProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) load(envFile.inputStream())
}

fun envString(key: String, default: String): String =
    envProps.getProperty(key, default)

fun envInt(key: String, default: Int): Int =
    envProps.getProperty(key)?.trim()?.toIntOrNull() ?: default

fun envFloat(key: String, default: Float): Float =
    envProps.getProperty(key)?.trim()?.toFloatOrNull() ?: default

android {
    namespace = "com.yumlensai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yumlensai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("int", "EXECUTION_TESTS_NUMBER",
            envInt("EXECUTION_TESTS_NUMBER", 35000).toString())
        buildConfigField("float", "MINIMAL_BATTERY_LEVEL",
            "${envFloat("MINIMAL_BATTERY_LEVEL", 0.03f)}f")
        buildConfigField("String", "BACKEND_URL",
            "\"${envString("BACKEND_URL", "http://192.168.1.4:5000")}\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    aaptOptions {
        noCompress += listOf("tflite")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.5.2")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // EXIF orientation reading (camera photos)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Accompanist permissions + system UI controller
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
