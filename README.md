# itinera-j

The JVM implementation of [Itinera](https://itinera-dev.org/), a workflow framework that keeps business rules separate from flow control, for Kotlin and for Java. It implements the [Itinera specification](https://github.com/itinera-dev/spec), whose vocabulary it uses.

A workflow is a list of steps. Each step holds one piece of business logic and says only how it ended: it succeeded, it failed with a reason, or it was skipped. Everything around the steps, such as retrying a failed attempt, passing data from one step to the next, lending a step the services it needs, and telling every reporter what happened, is the executor's work, done the same way for every workflow. Policies attached to a step or to the whole workflow change those decisions in one place, without touching the steps.

## Learning it

[The Itinera site](https://itinera-dev.org/) explains why Itinera exists, and [the specification](https://github.com/itinera-dev/spec) defines what every implementation does.

## Status

No version has been released yet. [CHANGELOG.md](https://github.com/itinera-dev/itinera-j/blob/main/CHANGELOG.md) will say what each release implements: the specification version, the tier, the capabilities each language claims and the conformance cases it passes, whose reports each release carries as assets.

## Using it

Once released, the artifacts will be on Maven Central under the group `org.itinera-dev`, all at one version, which the bill of materials names once. A Kotlin application adds `itinera-kotlin`, and a Java application `itinera-java`; both bring the core, `itinera`, and test support is `itinera-testing`, for an application's tests only:

```kotlin
dependencies {
    implementation(platform("org.itinera-dev:itinera-bom:VERSION"))
    implementation("org.itinera-dev:itinera-kotlin")
    testImplementation("org.itinera-dev:itinera-testing")
}
```

A Java application can also have javac report the mistakes of its annotated workflows as compile errors, such as a hook declared to return what its kind does not, by adding `itinera-checker` to its annotation processors; without it, the same mistakes are refused when the workflow is first described:

```kotlin
dependencies {
    annotationProcessor(platform("org.itinera-dev:itinera-bom:VERSION"))
    annotationProcessor("org.itinera-dev:itinera-checker")
}
```

A Kotlin application has KSP report them instead, by applying KSP's Gradle plugin, at the KSP version the checker's release notes name, and adding the checker to its symbol processors. KSP checks the workflows written in Kotlin, and those written in Java that extend a Kotlin class or name a part written in Kotlin; javac's checker checks the other Java workflows:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "KSP_VERSION"
}

dependencies {
    ksp(platform("org.itinera-dev:itinera-bom:VERSION"))
    ksp("org.itinera-dev:itinera-checker")
}
```

The artifacts run on JDK 17 and later. Java applications write synchronous workflows, and Kotlin applications synchronous ones or asynchronous ones on coroutines.

## Contributing

See the [contributor guide](https://github.com/itinera-dev/itinera-j/blob/main/docs/contributing.md) for how work arrives here, how to build and test, and the plan being implemented, and the [organisation's contributing guide](https://github.com/itinera-dev/.github/blob/main/CONTRIBUTING.md) for the rules every itinera-dev repository follows.

Built with AI under the terms of [A manifesto for software engineering with AI](https://marlon-sousa.com/blog/manifesto/); see [how Itinera is built](https://github.com/itinera-dev/.github/blob/main/CONTRIBUTING.md#how-itinera-is-built).

## License

Licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](https://github.com/itinera-dev/itinera-j/blob/main/LICENSE-APACHE) or <https://www.apache.org/licenses/LICENSE-2.0>)
- MIT license ([LICENSE-MIT](https://github.com/itinera-dev/itinera-j/blob/main/LICENSE-MIT) or <https://opensource.org/licenses/MIT>)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.
