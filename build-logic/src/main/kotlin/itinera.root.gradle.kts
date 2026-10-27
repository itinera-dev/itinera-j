plugins {
    base
    id("com.diffplug.spotless")
    id("itinera.raised-dependencies")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "build-logic/*.gradle.kts", "build-logic/src/**/*.gradle.kts")
        ktfmt(versionCatalogs.named("libs").findVersion("ktfmt").get().requiredVersion)
            .kotlinlangStyle()
    }
    kotlin {
        target("build-logic/src/**/*.kt")
        ktfmt(versionCatalogs.named("libs").findVersion("ktfmt").get().requiredVersion)
            .kotlinlangStyle()
    }
}

val checkManifests =
    tasks.register<CheckManifests>("checkManifests") {
        description = "Checks that the Kotlin and the Java conformance manifests agree."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        kotlinManifest = layout.projectDirectory.file("conformance.json")
        javaManifest = layout.projectDirectory.file("conformance-java.json")
    }

tasks.check {
    dependsOn(
        checkManifests,
        gradle.includedBuild("build-logic").task(":check"),
    )
}
