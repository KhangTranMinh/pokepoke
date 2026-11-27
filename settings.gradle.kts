pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        // Catalog của build-logic (SDK, Gradle plugin, thư viện convention plugin tự thêm); `libs` Gradle tự tạo từ gradle/libs.versions.toml.
        create("pokepoke") {
            from(files("gradle/pokepoke.versions.toml"))
        }
    }
}

rootProject.name = "PokePoke"
include(
    ":app",
    ":translation",
    ":core:testing",
    ":core:common-kotlin",
    ":core:common-android",
    ":core:designsystem",
    ":core:ui",
    ":core:network",
    ":core:connectivity",
    ":core:storage:api",
    ":core:storage:impl",
    ":core:securestorage:api",
    ":core:securestorage:impl",
    ":core:navigation",
    ":core:deeplink",
    ":business:domain",
    ":business:data",
    ":feature:onboarding:api",
    ":feature:onboarding:impl",
    ":feature:auth:api",
    ":feature:auth:impl",
    ":feature:main:api",
    ":feature:main:impl",
    ":feature:home:api",
    ":feature:home:impl",
    ":feature:details:api",
    ":feature:details:impl",
    ":feature:favourite:api",
    ":feature:favourite:impl",
    ":feature:profile:api",
    ":feature:profile:impl",
    ":debugtool",
)
