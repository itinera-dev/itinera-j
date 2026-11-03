// Google's mirror of Maven Central comes first in every build of this repository, since
// Central refuses requests from CI runners it rate-limits; the Plugin Portal and Central follow it.
pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "itinera-j"

include(
    "itinera",
    "itinera-kotlin",
    "itinera-java",
    "itinera-testing",
    "itinera-checker",
    "itinera-bom",
    "conformance-kotlin",
    "conformance-java",
)
