import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.khangtm.pokepoke.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)

    // Chạy lúc build (đọc configs/env/*.toml), nên là implementation chứ không phải compileOnly như các Gradle plugin.
    implementation(libs.tomlj)

    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "pokepoke.android.application"
            implementationClass = "com.khangtm.pokepoke.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "pokepoke.android.library"
            implementationClass = "com.khangtm.pokepoke.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "pokepoke.android.compose"
            implementationClass = "com.khangtm.pokepoke.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeatureApi") {
            id = "pokepoke.android.feature.api"
            implementationClass = "com.khangtm.pokepoke.buildlogic.AndroidFeatureApiConventionPlugin"
        }
        register("androidFeatureImpl") {
            id = "pokepoke.android.feature.impl"
            implementationClass = "com.khangtm.pokepoke.buildlogic.AndroidFeatureImplConventionPlugin"
        }
        register("hilt") {
            id = "pokepoke.hilt"
            implementationClass = "com.khangtm.pokepoke.buildlogic.HiltConventionPlugin"
        }
        register("room") {
            id = "pokepoke.room"
            implementationClass = "com.khangtm.pokepoke.buildlogic.RoomConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "pokepoke.kotlin.serialization"
            implementationClass = "com.khangtm.pokepoke.buildlogic.KotlinSerializationConventionPlugin"
        }
        register("jvmLibrary") {
            id = "pokepoke.jvm.library"
            implementationClass = "com.khangtm.pokepoke.buildlogic.JvmLibraryConventionPlugin"
        }
        register("kover") {
            id = "pokepoke.kover"
            implementationClass = "com.khangtm.pokepoke.buildlogic.KoverConventionPlugin"
        }
    }
}
