plugins { id("itinera.kotlin-library") }

description =
    "Itinera for Kotlin: asynchronous workflows on coroutines, Kotlin classes and data classes, and the reified forms."

dependencies {
    api(project(":itinera"))
    implementation(libs.kotlin.reflect)
}
