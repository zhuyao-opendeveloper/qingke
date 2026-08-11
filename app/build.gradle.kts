// 轻刻 LightMark - app 模块构建脚本
@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    // 读取本地签名配置（keystore.properties，已被 .gitignore 忽略，不入库）
    // 用 Kotlin 标准库解析，避免 java.util.Properties 在部分 Gradle Kotlin DSL 环境下解析失败
    val keystoreProps = mutableMapOf<String, String>()
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        keystoreFile.readLines().forEach { line ->
            val t = line.trim()
            if (t.isNotEmpty() && !t.startsWith("#") && '=' in t) {
                val (k, v) = t.split('=', limit = 2)
                keystoreProps[k.trim()] = v.trim()
            }
        }
    }

    namespace = "com.lightmark"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lightmark"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // 轻刻 v2.0.0 仍保留可选的 GitHub 同步 / OpenClaw 客户端 / 加密存储能力
        // （auth/AuthManager.kt、data/remote/* 等源码已包含对应网络与加密逻辑）。
        // 纯离线场景无需这些配置；在线同步相关能力由网页版 lightmark-web 协同提供。
    }

    signingConfigs {
        create("release") {
            storeType = "PKCS12"
            val envPath = System.getenv("KEYSTORE_PATH")
            if (envPath != null) {
                // CI：从 GitHub Secrets 注入的环境变量
                storeFile = file(envPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else if (keystoreProps.isNotEmpty()) {
                // 本地：keystore.properties
                storeFile = file(keystoreProps["storeFile"])
                storePassword = keystoreProps["storePassword"]
                keyAlias = keystoreProps["keyAlias"]
                keyPassword = keystoreProps["keyPassword"]
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.swiperefresh)

    // 网络与同步（GitHub 同步 / OpenClaw 客户端 / 加密存储）
    // 解决此前 :app:compileDebugKotlin 53 处 Unresolved reference
    implementation(libs.androidx.security.crypto)        // EncryptedSharedPreferences / MasterKey
    implementation(libs.retrofit)                        // Retrofit / GET / POST / Path / Query / Body ...
    implementation(libs.retrofit.converter.gson)         // GsonConverterFactory
    implementation(libs.okhttp)                          // OkHttpClient / Interceptor
    implementation(libs.gson)                            // Gson / GsonBuilder / SerializedName

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
