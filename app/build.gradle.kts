plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // 🌟 核心修复：添加这个插件，kapt 才能被识别
    id("kotlin-kapt")
}

android {
    namespace = "com.example.mizukisync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mizukisync"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 🌟 修复：响应警告，将 kotlinOptions 迁移到 compilerOptions
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.androidx.activity)

    // Glide 相关配置
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // 🌟 因为上面加了插件，这里现在不会再报错了
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}