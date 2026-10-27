// Libraries that tools bring with known advisories, raised wherever they are resolved.
dependencies {
    components {
        all<RaiseGuava> {
            params(versionCatalogs.named("libs").findVersion("guava").get().requiredVersion)
        }
    }
}
