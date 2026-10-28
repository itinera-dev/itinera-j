// A conformance runner written wholly in Java: a program run with `run`, never published.
plugins {
    id("itinera.jvm")
    application
}

// The cases and the report are named relative to where the command runs, not to the project.
tasks.named<JavaExec>("run") { workingDir = rootDir }
