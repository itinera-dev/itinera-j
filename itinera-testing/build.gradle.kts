plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera's test support: recording reporters, fake providers and scripted runs, for an application's tests."

dependencies {
    api(project(":itinera"))
    // Only the asynchronous parts need it, so a Java application's tests do not bring it.
    compileOnly(project(":itinera-kotlin"))
    testImplementation(project(":itinera-kotlin"))
    "compileFailTestImplementation"(libs.kctfork)
}
