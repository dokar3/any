import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}


kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
        languageVersion = KotlinVersion.fromVersion("2.3")
        apiVersion = KotlinVersion.fromVersion("2.3")
    }
}

android {
    namespace = "any.benchmark"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like your
        // release build (for example, with minification on). It"s signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions.managedDevices.allDevices {
        @Suppress("UnstableApiUsage")
        create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2Api31") {
            device = "Pixel 2"
            apiLevel = 31
            systemImageSource = "aosp"
        }
    }
}

dependencies {
    implementation(projects.data)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.espresso)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.macrobenchmark)
    implementation(libs.androidx.benchmark.common)
}

baselineProfile {
    managedDevices += "pixel2Api31"
    useConnectedDevices = false
}
