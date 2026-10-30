import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera's compile-time checks: annotated workflows' mistakes as compile errors, through KSP in Kotlin and javac in Java."

dependencies {
    // KSP gives its processors its own API, so applications never receive it from the checker.
    compileOnly(libs.ksp.api)
    testImplementation(project(":itinera-kotlin"))
    testImplementation(libs.jspecify)
    "compileFailTestImplementation"(project(":itinera-kotlin"))
    "compileFailTestImplementation"(project(":itinera-java"))
    "compileFailTestImplementation"(libs.jspecify)
    "compileFailTestImplementation"(libs.kctfork)
    "compileFailTestImplementation"(libs.kctfork.ksp)
}

// The checker reads itinera's annotations by name, so it uses none of itinera's API.
tasks.named<KotlinCompile>("compileKotlin") { compilerOptions.optIn.empty() }

// The fixtures are compiled as an application compiles them, keeping its parameters' names, and
// the parity tests compile their sources again, with the checker. javac starts the checker on its
// own wherever it finds it, so it stays off the class path the fixtures are first compiled with,
// as an application's checker stays off its class path.
tasks.named<JavaCompile>("compileCompileFailTestJava") {
    options.compilerArgs.add("-parameters")
    classpath -= files(tasks.named("jar"))
}

tasks.named<Test>("compileFailTest") {
    systemProperty("itinera.checker.sources", file("src/compileFailTest").path)
}

// KSP's API is no Java module, so the descriptor reads it as an automatic module, which Gradle
// would leave on the class path.
tasks.named<JavaCompile>("compileJava") {
    modularity.inferModulePath = false
    options.compilerArgumentProviders.add(
        objects.newInstance<ModulePath>().apply { jars.from(configurations.compileClasspath) }
    )
}

// The Kotlin sample compiles with KSP, which runs only in the compile-fail tests, where the
// compiler
// is on the class path.
dokka {
    dokkaSourceSets.configureEach {
        samples.from(fileTree("src/compileFailTest/kotlin") { include("**/samples/*.kt") })
    }
}
