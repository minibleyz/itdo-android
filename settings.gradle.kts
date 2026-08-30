pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Huawei Mobile Services (HMS Core SDK) — нужен для определения,
        // есть ли на устройстве HMS, когда Google Play Services нет (типично
        // для Huawei/Honor на EMUI/MagicOS без Google). См.
        // core/DeviceServices.kt.
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
rootProject.name = "ItdoAndroid"
include(":app")
