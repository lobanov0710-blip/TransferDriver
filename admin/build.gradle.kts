plugins {
    alias(libs.plugins.android.application)
}
android {
    namespace = "ru.transferservis.admin"
    compileSdk { version = release(37) }
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "ru.transferservis.admin"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ADMIN_API_BASE_URL", "\"https://uber-v3.lobanov0710.workers.dev/\"")
    }
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file(".local-signing/admin-debug.keystore")
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "ADMIN_API_BASE_URL", "\"https://uber-v3-staging.lobanov0710.workers.dev/\"")
        }
        release {
            optimization { enable = true }
        }
        create("staging") {
            initWith(getByName("release"))
            isDebuggable = false
            matchingFallbacks += listOf("release")
            versionNameSuffix = "-staging"
            buildConfigField("String", "ADMIN_API_BASE_URL", "\"https://uber-v3-staging.lobanov0710.workers.dev/\"")
        }
        create("qa") {
            initWith(getByName("staging"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            isDebuggable = false
            signingConfig = null // Owner signs this artifact locally with the persistent QA key.
            matchingFallbacks += listOf("staging", "release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
androidComponents {
    beforeVariants(selector().all()) {
        it.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
    onVariants(selector().withBuildType("qa")) { variant ->
        val code = providers.gradleProperty("adminQaVersionCode").map(String::toInt).orElse(2).map {
            require(it >= 2) { "Admin QA versionCode must be at least 2" }
            it
        }
        variant.outputs.forEach { it.versionCode.set(code) }
    }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okio)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
