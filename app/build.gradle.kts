import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.mahasiswa.sigma"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mahasiswa.sigma"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        manifestPlaceholders["MAPS_API_KEY"] = localProps.getProperty("MAPS_API_KEY", "")

        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL") ?: error("SUPABASE_URL not set in local.properties")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY") ?: error("SUPABASE_ANON_KEY not set in local.properties")}\"")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }
}

dependencies {
    // Force browser version to 1.8.0 to avoid API 36 requirement from transitive dependencies
    constraints {
        implementation("androidx.browser:browser:1.8.0")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.material)
    implementation(libs.coil.compose)
    implementation("com.google.code.gson:gson:2.13.2")
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation(libs.aandroidx.room.runtime)
    implementation(libs.aandroidx.room.ktx)
    ksp(libs.aandroidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.androidx.browser)
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
