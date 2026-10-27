plugins { id("itinera.bom") }

description = "The versions of every itinera artifact, so an application names the version once."

dependencies {
    constraints {
        api(project(":itinera"))
        api(project(":itinera-kotlin"))
        api(project(":itinera-testing"))
    }
}
