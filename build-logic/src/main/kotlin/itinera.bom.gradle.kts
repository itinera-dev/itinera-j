import com.vanniktech.maven.publish.JavaPlatform

plugins {
    `java-platform`
    id("com.diffplug.spotless")
    id("itinera.published")
}

spotless {
    kotlinGradle {
        ktfmt(versionCatalogs.named("libs").findVersion("ktfmt").get().requiredVersion)
            .kotlinlangStyle()
    }
}

mavenPublishing { configure(JavaPlatform()) }
