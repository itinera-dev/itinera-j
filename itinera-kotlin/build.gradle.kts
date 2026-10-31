plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera for Kotlin: asynchronous workflows on coroutines, Kotlin classes and data classes, and the reified forms."

dependencies {
    api(project(":itinera"))
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    "compileFailTestImplementation"(libs.kctfork)
}
