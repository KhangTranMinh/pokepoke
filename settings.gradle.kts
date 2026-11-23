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
}

rootProject.name = "PokePoke"
include(
    ":app",
    ":core:testing",
    ":core:common",
    ":core:designsystem",
    ":core:ui",
    ":core:network:api",
    ":core:network:impl",
    ":core:storage:api",
    ":core:storage:impl",
    ":core:securestorage:api",
    ":core:securestorage:impl",
    ":core:navigation:api",
    ":core:navigation:impl",
    ":core:deeplink:api",
    ":core:deeplink:impl",
    ":data:pokemon:api",
    ":data:pokemon:impl",
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
