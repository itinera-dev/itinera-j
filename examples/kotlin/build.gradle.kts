import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

// A Kotlin application reads itinera through Gradle's module metadata.
repositories {
    maven(rootDir.resolve("../build/package")) { content { includeGroup("org.itinera-dev") } }
    mavenCentral { content { excludeGroup("org.itinera-dev") } }
}

val itinera = providers.gradleProperty("itinera.version").get()

dependencies {
    implementation(platform("org.itinera-dev:itinera-bom:$itinera"))
    implementation("org.itinera-dev:itinera-kotlin")
    ksp(platform("org.itinera-dev:itinera-bom:$itinera"))
    ksp("org.itinera-dev:itinera-checker")
    testImplementation("org.itinera-dev:itinera-testing")
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The oldest JVM the artifacts run on, so that Gradle refuses one that needs a newer.
java { targetCompatibility = JavaVersion.VERSION_17 }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
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
    systemProperty("example.ksp", libs.versions.ksp.get())
    systemProperty(
        "example.source",
        file("src/main/kotlin/com/example/orders/FirstWorkflow.kt").path,
    )
}

// Every artifact the application and its tests receive publishes its sources as a variant of its
// module, which IDEs fetch.
val sources =
    configurations.testRuntimeClasspath.flatMap { classpath ->
        classpath.incoming
            .artifactView {
                withVariantReselection()
                componentFilter(::isItinera)
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
                }
            }
            .artifacts
            .resolvedArtifacts
    }

fun isItinera(component: ComponentIdentifier): Boolean =
    component is ModuleComponentIdentifier && component.group == "org.itinera-dev"

val checkSources =
    tasks.register("checkSources") {
        description = "Checks that each itinera artifact has a sources variant."
        val found = sources
        doLast {
            check(found.get().size == 3) {
                "itinera, itinera-kotlin and itinera-testing publish their sources."
            }
        }
    }

tasks.check { dependsOn(checkSources) }
