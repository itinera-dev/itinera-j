// What every project of the build compiled for the JVM shares, in Java or Kotlin, published or not.
plugins {
    java
    id("com.diffplug.spotless")
    id("itinera.raised-dependencies")
}

val libs = versionCatalogs.named("libs")

fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

java { toolchain { languageVersion = JavaLanguageVersion.of(BUILD_JDK) } }

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

spotless {
    kotlinGradle { ktfmt(version("ktfmt")).kotlinlangStyle() }
    java { googleJavaFormat(version("google-java-format")) }
}
