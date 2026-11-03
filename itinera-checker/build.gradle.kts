import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera's compile-time checks: annotated workflows' mistakes as compile errors, through javac in Java."

dependencies {
    testImplementation(project(":itinera-kotlin"))
    testImplementation(libs.jspecify)
    "compileFailTestImplementation"(project(":itinera-kotlin"))
    "compileFailTestImplementation"(project(":itinera-java"))
    "compileFailTestImplementation"(libs.jspecify)
}

// The checker reads itinera's annotations by name, so it uses none of itinera's API.
tasks.named<KotlinCompile>("compileKotlin") { compilerOptions.optIn.empty() }

// The fixtures are compiled as an application compiles them, keeping its parameters' names, and
// the parity tests compile their sources again, with the checker.
tasks.named<JavaCompile>("compileCompileFailTestJava") { options.compilerArgs.add("-parameters") }

tasks.named<Test>("compileFailTest") {
    systemProperty("itinera.checker.sources", file("src/compileFailTest").path)
}
