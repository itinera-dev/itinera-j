plugins { id("itinera.kotlin-library") }

description =
    "Itinera's core: values, events, the builder, descriptors, the engine and the synchronous executor."

dependencies { testImplementation(libs.jspecify) }

// The samples of annotated Kotlin classes run in itinera-kotlin's tests, where Kotlin classes are
// read.
dokka {
    dokkaSourceSets.configureEach {
        samples.from(
            rootProject.file(
                "itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/annotated/samples"
            )
        )
    }
}
