// Two applications, one written in Kotlin and one in Java, built against itinera as it is
// published. The `examples` task of the repository's build publishes every artifact to
// build/package, then builds, tests and runs them, passing the version as `itinera.version`.
pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

rootProject.name = "examples"

include("kotlin", "java")
