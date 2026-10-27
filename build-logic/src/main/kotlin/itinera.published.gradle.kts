// What every published artifact shares: Maven Central, signing and the POM it requires.
plugins { id("com.vanniktech.maven.publish") }

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name = project.name
        description = provider { project.description }
        url = "https://itinera-dev.org/"
        inceptionYear = "2026"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "itinera-dev"
                name = "The Itinera Authors"
                url = "https://github.com/itinera-dev"
            }
        }
        scm {
            url = "https://github.com/itinera-dev/itinera-j"
            connection = "scm:git:https://github.com/itinera-dev/itinera-j.git"
            developerConnection = "scm:git:ssh://git@github.com/itinera-dev/itinera-j.git"
        }
    }
}

// What would be published, written to the build directory, so CI checks every
// artifact, POM and module metadata file without uploading anything.
publishing {
    repositories {
        maven {
            name = "Package"
            url = uri(rootProject.layout.buildDirectory.dir("package"))
        }
    }
}
