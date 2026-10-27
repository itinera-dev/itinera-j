plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.maven.publish.gradle.plugin)
    implementation(libs.license.report.gradle.plugin)
    implementation(platform(libs.jackson.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

dependencyLocking { lockAllConfigurations() }

tasks.test { useJUnitPlatform() }

kotlin { compilerOptions { allWarningsAsErrors = true } }
