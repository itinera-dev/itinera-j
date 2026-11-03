import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// A conformance runner: a program run with `run`, never published.
plugins {
    id("itinera.kotlin-jvm")
    application
}

// A runner uses itinera's API as an application does, opting in to what is not listed yet.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.optIn.add("org.itineradev.itinera.ExperimentalItineraApi")
}

// The cases and the report are named relative to where the command runs, not to the project.
tasks.named<JavaExec>("run") { workingDir = rootDir }
