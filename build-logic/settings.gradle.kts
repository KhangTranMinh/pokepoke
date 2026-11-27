dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("pokepoke") {
            from(files("../gradle/pokepoke.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
