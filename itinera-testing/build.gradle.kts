plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera's test support: recording reporters, fake providers and scripted runs, for an application's tests."

dependencies {
    api(project(":itinera"))
    // Only the asynchronous parts need them, so a Java application's tests do not bring them.
    compileOnly(project(":itinera-kotlin"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(project(":itinera-kotlin"))
    "compileFailTestImplementation"(libs.kctfork)
}
