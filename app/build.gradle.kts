plugins {
    id("com.android.application")
    id("kotlin-android")
    alias(libs.plugins.google.ksp)
}

android {
    compileSdk = 35

    defaultConfig {
        namespace = "com.parishod.watomatic"
        applicationId = "com.parishod.watomatic"
        minSdk = 23
        targetSdk = 34
        versionCode = 29
        versionName = "1.29"

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    //Disable split language resources on .aab, necessary to allow the language changing
    //option to work
    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            // Includes the default ProGuard rules files that are packaged with
            // the Android Gradle plugin. To learn more, go to the section about
            // R8 configuration files.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    flavorDimensions += "version"
    productFlavors {
        create("GooglePlay") {
            dimension = "version"
            applicationId = "com.parishod.atomatic"
        }
        create("Default") {
            dimension = "version"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.preference.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.room.runtime)
    implementation(libs.work.runtime)
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    ksp(libs.room.compiler)
    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    implementation(libs.logging.interceptor)
    implementation(libs.sequence.layout)
    implementation(libs.browser)
    implementation(libs.security.crypto)
}
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
