plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    id("androidx.baselineprofile") version "1.4.1" apply false
    alias(libs.plugins.ksp) apply false
}