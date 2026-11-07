plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "ru.transferservis.driver"

    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "ru.transferservis.driver"

        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "DRIVER_API_BASE_URL",
            "\"https://uber-v3.lobanov0710.workers.dev/\""
        )

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "DRIVER_API_BASE_URL",
                "\"https://uber-v3-staging.lobanov0710.workers.dev/\""
            )
        }

        release {
            optimization {
                enable = true

                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    // Override Retrofit's vulnerable legacy HTTP dependencies.
    implementation(libs.okhttp)
    implementation(libs.okio)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.15.0"
        )
    )

    implementation(
        "com.google.firebase:firebase-messaging"
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "com.google.firebase:firebase-appcheck-playintegrity"
    )

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.espresso.core
    )

    androidTestImplementation(
        libs.ext.junit
    )
}
