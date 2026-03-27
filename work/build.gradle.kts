plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.verdant.work"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:ai"))
    implementation(project(":core:datastore"))
    implementation(project(":core:common"))
    implementation(project(":core:sms"))
    implementation(project(":core:health"))
    implementation(project(":core:devicestats"))
    implementation(project(":core:context"))
    implementation(project(":core:prediction"))
    implementation(project(":core:emotional"))
    implementation(project(":core:sync"))

    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.compiler.ext)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
