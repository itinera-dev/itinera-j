import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// What every Kotlin project of the build shares, published or not.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.detekt")
    id("com.diffplug.spotless")
    id("itinera.raised-dependencies")
}

val libs = versionCatalogs.named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

kotlin {
    jvmToolchain(BUILD_JDK)
    coreLibrariesVersion = version("kotlin-stdlib")
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(OLDEST_JDK.toString())
        apiVersion = KotlinVersion.fromVersion(version("kotlin-api"))
        allWarningsAsErrors = true
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.add("-Xjdk-release=$OLDEST_JDK")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = OLDEST_JDK
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

// The tests run on the JDK this property names, so CI can run them on each
// JDK applications may use.
val testJdk = providers.gradleProperty("itinera.test-jdk").map(String::toInt).orElse(BUILD_JDK)

tasks.test {
    useJUnitPlatform()
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = testJdk.map(JavaLanguageVersion::of)
    }
}

dependencyLocking { lockAllConfigurations() }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

spotless {
    kotlin { ktfmt(version("ktfmt")).kotlinlangStyle() }
    kotlinGradle { ktfmt(version("ktfmt")).kotlinlangStyle() }
    java { googleJavaFormat(version("google-java-format")) }
}
