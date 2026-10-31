import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// What every Kotlin project of the build shares, published or not.
plugins {
    id("itinera.jvm")
    id("org.jetbrains.kotlin.jvm")
    id("dev.detekt")
}

val libs = versionCatalogs.named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

kotlin {
    coreLibrariesVersion = version("kotlin-stdlib")
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(OLDEST_JDK.toString())
        apiVersion = KotlinVersion.fromVersion(version("kotlin-api"))
        allWarningsAsErrors = true
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.add("-Xjdk-release=$OLDEST_JDK")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

spotless { kotlin { ktfmt(version("ktfmt")).kotlinlangStyle() } }
