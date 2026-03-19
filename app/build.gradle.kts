plugins {
    id("com.android.application")
    id("kotlin-android")
    alias(libs.plugins.google.ksp)
    id("kotlin-parcelize")
    id("jacoco")
}

android {
    compileSdk = 35

    defaultConfig {
        namespace = "com.parishod.watomatic"
        applicationId = "com.parishod.watomatic"
        minSdk = 24
        targetSdk = 35
        versionCode = 35
        versionName = "1.35"

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
        getByName("debug") {
            enableUnitTestCoverage = true
        }
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
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    ksp(libs.room.compiler)
    implementation(libs.core.ktx)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    implementation(libs.logging.interceptor)
    implementation(libs.sequence.layout)
    implementation(libs.browser)
    implementation(libs.security.crypto)

    // Firebase and Google Sign-In
    // Add flavor-specific deps dynamically
    android.applicationVariants.all {
        val flavorName = this.flavorName
        if (flavorName.contains("GooglePlay", ignoreCase = true)) {
            add("implementation", platform(libs.firebase.bom))
            add("implementation", libs.firebase.auth)
            add("implementation", libs.firebase.firestore)
            add("implementation", libs.firebase.functions)
            add("implementation", libs.play.services.auth)
            add("implementation", libs.billing)
            add("implementation", libs.credentials)
            add("implementation", libs.credentials.play.services.auth)
            add("implementation", libs.googleid)
        }
    }
}

// Apply Google Services plugin only if building GooglePlay variant
gradle.startParameter.taskNames.any { task ->
    if (task.contains("GooglePlay", ignoreCase = true)) {
        apply(plugin = "com.google.gms.google-services")
        true
    } else {
        false
    }
}

// JaCoCo unit test coverage report for the Default/Debug variant
tasks.register<JacocoReport>("jacocoUnitTestReport") {
    dependsOn("testDefaultDebugUnitTest")
    group = "Reporting"
    description = "Generates JaCoCo unit test coverage report for DefaultDebug variant."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val excludes = listOf(
        "**/R.class", "**/R\$*.class",
        "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/databinding/**", "**/*_MembersInjector.class",
        "**/*_Factory.class", "**/*Directions*.*",
        "**/*\$\$serializer.class"
    )

    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/DefaultDebug/compileDefaultDebugJavaWithJavac/classes") {
        exclude(excludes)
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/DefaultDebug") {
        exclude(excludes)
    }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("**/*.exec", "**/*.ec")
        }
    )
}