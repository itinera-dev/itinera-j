plugins { id("itinera.conformance-runner") }

description = "The Kotlin conformance runner, which runs the cases of itinera-dev/conformance."

application { mainClass = "org.itineradev.itinera.conformance.MainKt" }

dependencies {
    implementation(project(":itinera"))
    implementation(project(":itinera-kotlin"))
    implementation(project(":itinera-testing"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.cucumber.bom))
    implementation(libs.cucumber.java)
    implementation(libs.cucumber.picocontainer)
    implementation(libs.cucumber.tag.expressions)
    implementation(libs.jackson.runners.databind)
}
