import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/**
 * The server the app talks to: `paribabi.serverUrl` in local.properties (never committed), the address of the Mac
 * on the network for a real phone ("http://192.168.1.20:8080"). Empty: the emulator reaches the Mac by itself.
 */
val serverUrl: String =
    Properties()
        .apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
        .getProperty("paribabi.serverUrl")
        .orEmpty()

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "bighid.romatazok.adherentapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "bighid.paribabi.adherent"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}