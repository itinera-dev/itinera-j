import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// An artifact of itinera: published, documented and with its public API recorded.
plugins {
    id("itinera.kotlin-jvm")
    id("itinera.dokka")
    id("itinera.published")
    id("com.github.jk1.dependency-license-report")
}

val libs = versionCatalogs.named("libs")

kotlin {
    explicitApi()
    @OptIn(ExperimentalAbiValidation::class) abiValidation()
}

// itinera's own code uses its API whatever its proposal; applications and tests opt in.
tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions.optIn.add("org.itineradev.itinera.ExperimentalItineraApi")
}

// Each artifact is a Java module, named after it, whose descriptor javac compiles
// with the classes Kotlin compiled.
val patchModule =
    objects.newInstance<PatchModule>().apply {
        module = "org.itineradev." + project.name.replace('-', '.')
        classes = tasks.named<KotlinCompile>("compileKotlin").flatMap { it.destinationDirectory }
    }

tasks.named<JavaCompile>("compileJava") { options.compilerArgumentProviders.add(patchModule) }

// What applications receive from this artifact.
licenseReport {
    configurations = arrayOf("runtimeClasspath")
    filters = arrayOf(LicenseBundleNormalizer())
    allowedLicensesFile = rootProject.file("config/allowed-licences.json")
}

tasks.check { dependsOn(tasks.named("checkLicense")) }

dokka {
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        jdkVersion = OLDEST_JDK
        includes.from(fileTree(projectDir) { include("Module.md") })
        // Samples are functions in the tests, which run them.
        samples.from(fileTree("src/test/kotlin") { include("**/samples/*.kt") })
        perPackageOption {
            matchingRegex = """org\.itineradev\.itinera\.engine(\..*)?"""
            suppress = true
        }
    }
}

// Every artifact's javadoc jar holds the whole reference, which the root project gathers,
// so that javadoc.io shows all of it whichever artifact a reader starts from.
mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka(rootProject.tasks.named("dokkaGeneratePublicationHtml")),
            sourcesJar = SourcesJar.Sources(),
        )
    )
}
