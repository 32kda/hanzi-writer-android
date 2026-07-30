plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

android {
    namespace = "org.openhanziwriter.app"
    compileSdk = 34

    val keystorePropertiesFile = file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        val props = Properties().apply { load(keystorePropertiesFile.inputStream()) }
        signingConfigs {
            create("release") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "org.openhanziwriter.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4

        versionName = "1.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // SplashScreen
    implementation(libs.core.splashscreen)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.junit5.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

tasks.register<Exec>("generateCharacterDb") {
    description = "Generates the pre-populated characters.db from all.json using Python"
    group = "database"

    workingDir = rootProject.projectDir
    commandLine("python", "build_scripts/generate_character_db.py")

    // Skip if db already exists and is newer than the source data
    onlyIf {
        val dbFile = file("src/main/assets/databases/characters.db")
        val sourceFile = file("${rootProject.projectDir}/build_scripts/data/all.json")
        !dbFile.exists() || !sourceFile.exists() || dbFile.lastModified() < sourceFile.lastModified()
    }
}

// Generate the database before the Android build starts
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("generateCharacterDb")
}

tasks.register("copyTestDb") {
    dependsOn("generateCharacterDb")
    doLast {
        copy {
            from("src/main/assets/databases/characters.db")
            into("src/test/resources/databases")
        }
    }
}
tasks.matching { it.name.matches(Regex("generate\\w*UnitTestResources")) }.configureEach {
    dependsOn("copyTestDb")
}

kapt {
    correctErrorTypes = true
}
