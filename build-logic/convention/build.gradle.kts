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
    compileOnly(pokepoke.android.gradlePlugin)
    compileOnly(pokepoke.kotlin.gradlePlugin)
    compileOnly(pokepoke.kotlin.compose.gradlePlugin)
    compileOnly(pokepoke.ksp.gradlePlugin)
    compileOnly(pokepoke.hilt.gradlePlugin)
    compileOnly(pokepoke.room.gradlePlugin)
    compileOnly(pokepoke.kover.gradlePlugin)
    compileOnly(pokepoke.roborazzi.gradlePlugin)

    // Chạy lúc build (đọc configs/env/*.toml), nên là implementation chứ không phải compileOnly như các Gradle plugin.
    implementation(pokepoke.tomlj)

    testImplementation(pokepoke.junit)
}

// Test đọc file thật của repo (catalog, structure.toml, settings): khai báo là input để sửa file thì test chạy lại.
tasks.test {
    val repoRoot = layout.projectDirectory.dir("../..")
    inputs.files(
        repoRoot.file("gradle/pokepoke.versions.toml"),
        repoRoot.file("gradle/libs.versions.toml"),
        repoRoot.file("gradle/core.versions.toml"),
        repoRoot.file("configs/build/structure.toml"),
        repoRoot.file("settings.gradle.kts"),
    ).withPropertyName("repoFiles").withPathSensitivity(PathSensitivity.RELATIVE)
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
