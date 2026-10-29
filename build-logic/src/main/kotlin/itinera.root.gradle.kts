plugins {
    base
    id("com.diffplug.spotless")
    id("itinera.raised-dependencies")
}

val libs = versionCatalogs.named("libs")

spotless {
    kotlinGradle {
        target(
            "*.gradle.kts",
            "build-logic/*.gradle.kts",
            "build-logic/src/**/*.gradle.kts",
            "examples/**/*.gradle.kts",
        )
        ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
    }
    kotlin {
        target("build-logic/src/**/*.kt", "examples/*/src/**/*.kt")
        ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
    }
    java {
        target("examples/*/src/**/*.java")
        googleJavaFormat(libs.findVersion("google-java-format").get().requiredVersion)
    }
}

val checkManifests =
    tasks.register<CheckManifests>("checkManifests") {
        description = "Checks that the Kotlin and the Java conformance manifests agree."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        kotlinManifest = layout.projectDirectory.file("conformance.json")
        javaManifest = layout.projectDirectory.file("conformance-java.json")
    }

fun publication(project: String): String = ":$project:publishAllPublicationsToPackageRepository"

// Applications of their own, which read itinera as published, POMs, module metadata and
// documentation jars included.
tasks.register<Exec>("examples") {
    description =
        "Builds and tests the Kotlin and the Java examples against the published artifacts."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    val published =
        listOf(
            "itinera",
            "itinera-kotlin",
            "itinera-java",
            "itinera-testing",
            "itinera-checker",
            "itinera-bom",
        )
    dependsOn(published.map(::publication))
    val windows = System.getProperty("os.name").startsWith("Windows")
    val wrapper = if (windows) "gradlew.bat" else "gradlew"
    commandLine(
        rootDir.resolve(wrapper),
        "--project-dir=examples",
        "--no-daemon",
        "-Pitinera.version=$version",
        "check",
    )
}

tasks.check {
    dependsOn(
        checkManifests,
        gradle.includedBuild("build-logic").task(":check"),
    )
}
