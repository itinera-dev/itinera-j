plugins { id("itinera.java-conformance-runner") }

description =
    "The Java conformance runner, written wholly in Java, which runs the cases of itinera-dev/conformance."

application { mainClass = "org.itineradev.itinera.conformance.Main" }

dependencies {
    implementation(project(":itinera-java"))
    implementation(project(":itinera-testing"))
    implementation(platform(libs.cucumber.bom))
    implementation(libs.cucumber.java)
    implementation(libs.cucumber.picocontainer)
    implementation(libs.cucumber.tag.expressions)
    implementation(libs.jackson.runners.databind)
}
