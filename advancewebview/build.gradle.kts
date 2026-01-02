plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.dev.jesen.advancewebview"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.dev.jesen.advancewebview"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // WebView调试开关（发布版关闭）
        buildConfigField("boolean", "ENABLE_ADVANCE_DEBUG", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_ADVANCE_DEBUG", "false")
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // 资源配置（允许访问assets目录）
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
    viewBinding { enable = true }
    buildFeatures.buildConfig = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // WebView 相关（Android自带，无需额外引入，仅需兼容配置）
    implementation(libs.androidx.webkit) // 官方WebView兼容库
    // Gson（用于原生-JS通信数据解析）
    implementation(libs.google.gson)

}