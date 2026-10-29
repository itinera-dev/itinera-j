plugins {
    id("itinera.kotlin-library")
    id("itinera.compile-fail-tests")
}

description =
    "Itinera's core: values, events, the builder, descriptors, the engine and the synchronous executor."

dependencies {
    testImplementation(libs.jspecify)
    "compileFailTestImplementation"(libs.kctfork)
}

// The samples of annotated Kotlin classes and of data classes run in itinera-kotlin's tests, where
// Kotlin classes are read, and the sample of opting in runs in itinera-testing's, whose API is the
// only one not claimed yet.
dokka {
    dokkaSourceSets.configureEach {
        samples.from(
            rootProject.file(
                "itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/annotated/samples"
            ),
            rootProject.file(
                "itinera-kotlin/src/test/kotlin/org/itineradev/itinera/journey/kotlin/samples"
            ),
            rootProject.file(
                "itinera-testing/src/test/kotlin/org/itineradev/itinera/step/testing/samples"
            ),
        )
    }
}
