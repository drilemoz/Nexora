plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "br.com.adriedson.nexora"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.adriedson.nexora"
        minSdk = 33 // Requisito atual do módulo Android oficial do llama.cpp
        targetSdk = 35
        versionCode = 20
        versionName = "0.1.0-alpha.20"
        ndk { abiFilters += "arm64-v8a" }
    }

    // O keystore fica fora do repositório. Credenciais chegam pelo ambiente do runner.
    // Sem credenciais, o Gradle usa a assinatura Debug padrão apenas para testes locais.
    val stableKeyFile = System.getenv("NEXORA_KEYSTORE_PATH")
        ?.takeIf { it.isNotBlank() }?.let { file(it) }
    val stableStorePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    val stableKeyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
    val stableKeyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    val hasStableSigning = stableKeyFile?.isFile == true &&
        stableStorePassword != null && stableKeyAlias != null && stableKeyPassword != null

    signingConfigs {
        if (hasStableSigning) {
            create("stable") {
                storeFile = stableKeyFile
                storePassword = stableStorePassword
                keyAlias = stableKeyAlias
                keyPassword = stableKeyPassword
                storeType = "pkcs12"
            }
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            if (hasStableSigning) signingConfig = signingConfigs.getByName("stable")
        }
        getByName("release") {
            isMinifyEnabled = false
            if (hasStableSigning) signingConfig = signingConfigs.getByName("stable")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    packaging { jniLibs { useLegacyPackaging = true } }
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation("androidx.activity:activity-ktx:1.12.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation(libs.junit)
}
