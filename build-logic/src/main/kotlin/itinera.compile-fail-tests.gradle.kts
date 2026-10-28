import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

// The compile-fail tests: snippets that the types refuse, each beside a twin that compiles. They
// are a test suite of their own, so the compiler they need stays off the other tests' class path,
// where the oldest Kotlin standard library itinera supports runs.
plugins { id("itinera.kotlin-jvm") }

val libs = versionCatalogs.named("libs")

val compileFailTest =
    testing.suites.register<JvmTestSuite>("compileFailTest") {
        useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)
        dependencies { implementation(project()) }
    }

// kctfork compiles the snippets with the Kotlin compiler that builds itinera, not with the one it
// was built against.
val kotlinVersion = getKotlinPluginVersion()

configurations
    .matching { it.name.startsWith("compileFailTest") }
    .configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(kotlinVersion)
        }
    }

// The required static analysis runs the detekt task alone, which reads only the main and test
// sources unless told otherwise.
detekt { source.from("src/compileFailTest/kotlin") }

tasks.check { dependsOn(compileFailTest) }
