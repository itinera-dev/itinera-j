dependencyResolutionManagement {
    repositories {
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
