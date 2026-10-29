plugins { java }

// A Java application built as Maven builds it: itinera is read through its POMs alone.
repositories {
    maven(rootDir.resolve("../build/package")) {
        content { includeGroup("org.itinera-dev") }
        metadataSources {
            mavenPom()
            ignoreGradleMetadataRedirection()
        }
    }
    mavenCentral { content { excludeGroup("org.itinera-dev") } }
}

val itinera = providers.gradleProperty("itinera.version").get()

val documentation = configurations.register("documentation") { isTransitive = false }

dependencies {
    implementation(platform("org.itinera-dev:itinera-bom:$itinera"))
    implementation("org.itinera-dev:itinera-java")
    annotationProcessor(platform("org.itinera-dev:itinera-bom:$itinera"))
    annotationProcessor("org.itinera-dev:itinera-checker")
    testImplementation("org.itinera-dev:itinera-testing")
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    listOf("itinera", "itinera-kotlin", "itinera-java", "itinera-testing", "itinera-checker")
        .forEach { artifact ->
            documentation("org.itinera-dev:$artifact:$itinera:sources")
            documentation("org.itinera-dev:$artifact:$itinera:javadoc")
        }
}

// The oldest JVM the artifacts run on, so that Gradle refuses one that needs a newer.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.compilerArgs.add("-Werror")
}

dependencyLocking {
    lockAllConfigurations()
    ignoredDependencies.add("org.itinera-dev:*")
}

tasks.test {
    useJUnitPlatform()
    val readme = rootDir.resolve("../README.md")
    inputs.file(readme).withPropertyName("readme").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("example.readme", readme.path)
    systemProperty(
        "example.source",
        file("src/main/java/com/example/orders/FirstWorkflow.java").path,
    )
}

// What only asynchronous Kotlin needs never reaches a Java application, nor its tests.
val checkClasspath =
    tasks.register("checkClasspath") {
        description = "Checks that neither kotlin-reflect nor kotlinx.coroutines reaches Java."
        // Local, so that the configuration cache stores the task without the build script.
        fun componentName(artifact: ResolvedArtifactResult): String =
            artifact.id.componentIdentifier.displayName
        fun isKotlinOnly(component: String): Boolean =
            component.startsWith("org.jetbrains.kotlin:kotlin-reflect:") ||
                component.startsWith("org.jetbrains.kotlinx:kotlinx-coroutines")
        val received =
            configurations.testRuntimeClasspath.flatMap { classpath ->
                classpath.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                    artifacts.map(::componentName)
                }
            }
        doLast {
            val kotlinOnly = received.get().filter(::isKotlinOnly)
            check(kotlinOnly.isEmpty()) { "A Java application receives $kotlinOnly." }
        }
    }

// Each artifact publishes its sources and its API reference beside its POM, as Maven Central
// requires.
val checkDocumentation =
    tasks.register("checkDocumentation") {
        description = "Checks that each itinera artifact has a sources and a javadoc jar."
        val jars = documentation.map { it.incoming.files }
        doLast { check(jars.get().files.size == 10) { "The 5 artifacts publish 10 jars." } }
    }

tasks.check { dependsOn(checkClasspath, checkDocumentation) }
