// What every Dokka build shares, the artifacts' own and the reference that gathers them.
plugins {
    id("org.jetbrains.dokka")
    // Without it, as in the root project, a Maven BOM is not read as a platform.
    `jvm-ecosystem`
}

val libs = versionCatalogs.named("libs")

// The generator, and the plugins a publication adds, such as the page that gathers the
// artifacts, are raised past the advisories on the versions they ask for.
listOf("dokkaHtmlGeneratorRuntime", "dokkaHtmlPublicationPlugin").forEach { configuration ->
    dependencies {
        configuration(platform(libs.findLibrary("jackson-bom").get()))
        constraints {
            configuration(libs.findLibrary("freemarker").get())
            configuration(libs.findLibrary("jsoup").get())
        }
    }
}

dokka { dokkaPublications.html { failOnWarning = true } }
