import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("dev.detekt")
    id("com.diffplug.spotless")
    id("itinera.published")
    id("com.github.jk1.dependency-license-report")
}

val libs = versionCatalogs.named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

// The newest LTS builds; applications may run on JDK 17 or later.
val buildJdk = 25
val oldestJdk = 17

kotlin {
    jvmToolchain(buildJdk)
    explicitApi()
    @OptIn(ExperimentalAbiValidation::class) abiValidation()
    coreLibrariesVersion = version("kotlin-stdlib")
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(oldestJdk.toString())
        apiVersion = KotlinVersion.fromVersion(version("kotlin-api"))
        allWarningsAsErrors = true
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.add("-Xjdk-release=$oldestJdk")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = oldestJdk
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

// Each artifact is a Java module, named after it, whose descriptor javac compiles
// with the classes Kotlin compiled.
val patchModule =
    objects.newInstance<PatchModule>().apply {
        module = "org.itineradev." + project.name.replace('-', '.')
        classes = tasks.named<KotlinCompile>("compileKotlin").flatMap { it.destinationDirectory }
    }

tasks.named<JavaCompile>("compileJava") { options.compilerArgumentProviders.add(patchModule) }

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    dokkaHtmlGeneratorRuntime(platform(libs.findLibrary("jackson-bom").get()))
    constraints {
        dokkaHtmlGeneratorRuntime(libs.findLibrary("freemarker").get())
        dokkaHtmlGeneratorRuntime(libs.findLibrary("jsoup").get())
    }
}

// The tests run on the JDK this property names, so CI can run them on each
// JDK applications may use.
val testJdk = providers.gradleProperty("itinera.test-jdk").map(String::toInt).orElse(buildJdk)

tasks.test {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = testJdk.map(JavaLanguageVersion::of)
    }
}

dependencyLocking { lockAllConfigurations() }

// What applications receive from this artifact.
licenseReport {
    configurations = arrayOf("runtimeClasspath")
    filters = arrayOf(LicenseBundleNormalizer())
    allowedLicensesFile = rootProject.file("config/allowed-licences.json")
}

tasks.check { dependsOn(tasks.named("checkLicense")) }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

spotless {
    kotlin { ktfmt(version("ktfmt")).kotlinlangStyle() }
    kotlinGradle { ktfmt(version("ktfmt")).kotlinlangStyle() }
    java { googleJavaFormat(version("google-java-format")) }
}

dokka {
    dokkaPublications.html { failOnWarning = true }
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        jdkVersion = oldestJdk
        includes.from(fileTree(projectDir) { include("Module.md") })
    }
}

mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        )
    )
}
