plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun prop(name: String): String =
    keystoreProperties.getProperty(name)
        ?: error("Missing '$name' in keystore.properties")

android {
    namespace = "com.codeonyx.bcvaqui"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codeonyx.bcvaqui"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.3.0"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(prop("storeFile"))
                storePassword = prop("storePassword")
                keyAlias = prop("keyAlias")
                keyPassword = prop("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Nunca firmar el release con la clave de debug: ese APK no podría
            // actualizar el que los usuarios ya tienen instalado.
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Falla con un mensaje claro si se intenta empaquetar el release sin keystore.
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.project == project && Regex("^(assemble|bundle|package|sign)Release.*").matches(task.name)
    }
    if (buildsRelease && !keystorePropertiesFile.exists()) {
        throw GradleException(
            "Falta keystore.properties en la raíz del proyecto. El release no se firma con la clave " +
                "de debug. Copia keystore.properties.example, complétalo y vuelve a compilar."
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.splashscreen)

    debugImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation("junit:junit:4.13.2")
}
