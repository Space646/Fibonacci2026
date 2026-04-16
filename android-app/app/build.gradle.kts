plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.fibonacci.fibohealth"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.fibonacci.fibohealth"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    room { schemaDirectory("$projectDir/schemas") }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.nav.compose)
    implementation(libs.nav.suite)
    implementation(libs.adaptive.layout)
    implementation(libs.adaptive.navigation)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.health.connect)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.serialization.json)
}
