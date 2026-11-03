plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera for Java: the core, and what makes its API read naturally from Java, for synchronous workflows."

dependencies {
    api(project(":itinera"))
    // The compile-fail test shows that Java cannot call what itinera-kotlin keeps from it.
    "compileFailTestImplementation"(project(":itinera-kotlin"))
    // Its reference shows its API as Java applications, its only readers, call it.
    dokkaPlugin(libs.dokka.java.signatures)
}
