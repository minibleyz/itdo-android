plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.itdo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.itdo.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Базовый URL API соцсети itdo.
        buildConfigField("String", "API_BASE_URL", "\"https://itdo.bleyzos.ru/api/\"")
        // Базовый URL самого сайта (без /api/) — нужен для WebView-страниц,
        // которых нет в REST API и которые проще переиспользовать из веба
        // (сейчас — ai-agent.html, см. ui/agent/AgentScreen.kt).
        buildConfigField("String", "SITE_BASE_URL", "\"https://itdo.bleyzos.ru/\"")
        // hCaptcha site key — тот же, что используется на вебе (см. login.html).
        buildConfigField("String", "HCAPTCHA_SITE_KEY", "\"5f92e784-d356-42ce-8244-5672a768ae26\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Material 3 Expressive API (ButtonGroup, expressive shapes/motion, новые
    // list items и т.д.) доступны только в alpha-ветке 1.5.0 — стабильный
    // material3 сейчас 1.4.0 и Expressive не содержит. Версия зафиксирована
    // явно (а не через BOM), т.к. BOM ещё не тянет alpha-релизы material3.
    implementation("androidx.compose.material3:material3:1.5.0-alpha24")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0")

    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
