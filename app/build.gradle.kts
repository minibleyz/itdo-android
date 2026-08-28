plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'ru.itdo.app'
    compileSdk 35

    defaultConfig {
        applicationId "ru.itdo.app"
        minSdk 21
        targetSdk 35
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_20
        targetCompatibility JavaVersion.VERSION_20
    }
    
    kotlinOptions {
        jvmTarget = '20'
    }
    
    buildFeatures {
        compose true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.14'
    }
    
    packaging {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
}

dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.15.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.9.0'
    implementation 'androidx.activity:activity-compose:1.10.0'
    
    // Material 3 Expressive (альфа-версия)
    implementation "androidx.compose.material3:material3:1.5.0-alpha08"
    
    // Compose BOM для управления версиями
    implementation platform('androidx.compose:compose-bom:2025.02.00')
    
    // Compose основные библиотеки
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.runtime:runtime'
    implementation 'androidx.compose.runtime:runtime-livedata'
    
    // Compose Tooling (для разработки)
    debugImplementation 'androidx.compose.ui:ui-tooling'
    debugImplementation 'androidx.compose.ui:ui-test-manifest'
    
    // Compose Foundation
    implementation 'androidx.compose.foundation:foundation'
    implementation 'androidx.compose.foundation:foundation-layout'
    implementation 'androidx.compose.animation:animation'
    implementation 'androidx.compose.animation:animation-core'
    
    // Navigation
    implementation 'androidx.navigation:navigation-compose:2.8.9'
    
    // ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0'
    
    // Retrofit (сетевые запросы)
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0'
    
    // Dependency Injection (Hilt)
    implementation 'com.google.dagger:hilt-android:2.55'
    implementation 'androidx.hilt:hilt-navigation-compose:1.2.0'
    kapt 'com.google.dagger:hilt-compiler:2.55'
    
    // Coil (загрузка изображений)
    implementation 'io.coil-kt.coil3:coil-compose:3.0.4'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
}
