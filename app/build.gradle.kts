plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "app.echo"
    compileSdk = 34
    defaultConfig {
        applicationId = "app.echo"
        minSdk = 26
        targetSdk = 34
        ndk { abiFilters += "arm64-v8a" }
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += listOf("onnx", "wav") }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.json:json:20240303")
}
