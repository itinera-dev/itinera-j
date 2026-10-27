plugins { id("itinera.kotlin-library") }

description =
    "Itinera's core: values, events, the builder, descriptors, the engine and the synchronous executor."

dependencies { testImplementation(libs.jspecify) }
