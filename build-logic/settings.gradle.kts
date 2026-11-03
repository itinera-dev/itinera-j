pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            // The weekly run builds with the newest Kotlin, given as this property.
            providers.gradleProperty("itinera.next-kotlin").orNull?.let { version("kotlin", it) }
        }
    }
}

rootProject.name = "build-logic"
