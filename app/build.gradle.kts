plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ru.transferservis.driver"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.transferservis.driver"

        minSdk = 26
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.espresso.core
    )

    androidTestImplementation(
        libs.ext.junit
    )
}