# Contributing to itinera-j

This guide covers what is specific to the JVM implementation. The rules every itinera-dev repository follows are in the [organisation's contributing guide](https://github.com/itinera-dev/.github/blob/main/CONTRIBUTING.md), and the full process is in [PROCESS.md](https://github.com/itinera-dev/spec/blob/main/PROCESS.md).

## How work arrives here

What Itinera does is defined in [itinera-dev/spec](https://github.com/itinera-dev/spec): the proposals (the PRDs) and the behaviour specification. How the JVM does it is defined here, in **tech specs**.

1. Each accepted proposal gets an **implementation issue** here, labelled `implements-proposal`, covering Kotlin and Java together. An issue opened before its proposal is accepted also carries `waiting for spec`, and no pull request is opened for it until that label is removed.
2. The **tech spec** is agreed in that issue: API shape in Kotlin and in Java, artifacts and packages touched, types, annotations, builder methods, error handling, tests and the definition of done.
3. The implementing pull request adds the tech spec as `docs/specs/NNNN-short-name.md`, where `NNNN` is the proposal's number, together with the code.
4. The issue closes when the proposal's [conformance](https://github.com/itinera-dev/conformance) cases pass in both runners.

Bugs and API questions can be opened here directly; changes to behaviour are proposals in the spec repository.

## The plan being implemented

Tier 1 is implemented following the [tier 1 plan](tier-1-plan.md), in stages tracked in [#1](https://github.com/itinera-dev/itinera-j/issues/1). The plan holds who the implementation is for, the architecture, the tests, the order of work and the tooling; each proposal's tech spec holds what concerns that proposal alone. From stage 3, `architecture.md` draws what has been built, and is kept true by every pull request.

## The build

One Gradle build, with the Kotlin DSL, a module per artifact, and every artifact at the version in `gradle.properties`:

| Module | Published | Holds |
|---|---|---|
| `itinera` | yes | The core, written in Kotlin, which every application depends on: values, events, the synchronous parts, the builder, descriptors, the engine and `LocalExecutor`. It brings Kotlin's standard library only |
| `itinera-kotlin` | yes | What Kotlin applications add: asynchronous workflows on coroutines, the reader and value walk for Kotlin classes, and the reified forms. It brings kotlin-reflect and kotlinx.coroutines |
| `itinera-java` | yes | What Java applications add: the Java face of the core, which grows from stage 3 with whatever Java finds awkward in the core |
| `itinera-testing` | yes | Test support, which an application adds to its tests only |
| `itinera-checker` | yes, from stage 12 | Compile-time checks of the annotations, for KSP and javac |
| `itinera-bom` | yes | The bill of materials: the version of every artifact above |
| `conformance` | no | The Kotlin conformance runner |
| `conformance-java` | no | The Java conformance runner, written wholly in Java |

Each published artifact is a Java module named after it, such as `org.itineradev.itinera` and `org.itineradev.itinera.kotlin`, with its descriptor in `src/main/java/module-info.java`. Kotlin compiles the module's classes, and javac then compiles the descriptor with those classes patched into the module.

What the modules share lives in `build-logic`, a build included by `settings.gradle.kts`, as convention plugins:

- `itinera.jvm`, what every project compiled for the JVM shares, in Java or Kotlin, published or not: the JDK 25 toolchain, javac compiling for JVM 17 with every warning an error, JUnit on the JDK `-Pitinera.test-jdk` names, Spotless for Java and the build script, and dependency locking;
- `itinera.kotlin-jvm`, applied by every Kotlin project on top of `itinera.jvm`: Kotlin compiled for JVM 17 with `apiVersion` one release behind the compiler and the matching standard library, warnings as errors, ktfmt and detekt;
- `itinera.kotlin-library`, applied by every published library on top of `itinera.kotlin-jvm`: explicit API mode, the module descriptor, Dokka, the ABI dump and the licence check;
- `itinera.conformance-runner`, applied by the Kotlin runner on top of `itinera.kotlin-jvm`: the `application` plugin, the opt-in to `@ExperimentalItineraApi`, and `run` started from the repository's root, so the paths a runner reads are relative to where the command runs;
- `itinera.java-conformance-runner`, applied by the Java runner on top of `itinera.jvm`: the `application` plugin, and `run` started from the repository's root;
- `itinera.published`, applied by every published artifact: Maven Central through the `com.vanniktech.maven.publish` plugin, the POM Central requires, signing when a key is given, and the `Package` repository under `build/package`;
- `itinera.bom`, applied by `itinera-bom`;
- `itinera.raised-dependencies`, applied by `itinera.jvm`, `itinera.bom` and `itinera.root`: `RaiseGuava`, which raises Guava wherever it is resolved, as "Dependencies" says;
- `itinera.root`, applied by the root project: the formatting of the build's own files and the check that the two manifests agree, `CheckManifests` over `ManifestAgreement`, which `build-logic`'s own tests check.

`gradle/libs.versions.toml` holds every version: Kotlin, the standard library applications get, and every plugin and library, but for two that Gradle reads before the catalogue exists: the toolchain resolver plugin in `settings.gradle.kts`, and Gradle itself in `gradle/wrapper/gradle-wrapper.properties`.

## Building and testing

The build needs a JDK 17 or later to start Gradle; Gradle's toolchains then use JDK 25 to compile, and the JDK the tests run on. A toolchain that is not installed is downloaded through the Foojay resolver. Apart from that, `./gradlew` fetches everything, Gradle included, at the versions the repository pins.

Every one of these is a required check in CI, each one Gradle task:

- `./gradlew spotlessCheck`: ktfmt in its Kotlin style for Kotlin and the build scripts, google-java-format for Java, through Spotless;
- `./gradlew detekt`: detekt on its default rules with the changes in `config/detekt/detekt.yml`;
- `./gradlew assemble`: compiles everything, with every warning an error;
- `./gradlew test`: JUnit Jupiter, on JDK 25 unless `-Pitinera.test-jdk=N` names another; CI runs it on JDK 17, 21 and 25 on Linux, and on JDK 25 on Windows and macOS;
- `./gradlew compileFailTest`: the compile-fail tests, snippets that the types refuse, each beside a twin that compiles, run before pushing only when a change touches a public signature, a type that refuses something at compile time, the suites themselves or what compiles them;
- `./gradlew checkKotlinAbi`: compares each artifact's public API with its dump under `api/`, which `./gradlew updateKotlinAbi` writes, so every change to what an artifact exposes shows in a pull request;
- `./gradlew dokkaGenerate`: the API reference, with Dokka, every public item documented and warnings as errors;
- `./gradlew publishAllPublicationsToPackageRepository`: everything Maven Central would receive, written to `build/package` instead: jars, sources and javadoc jars, POMs and Gradle module metadata;
- `./gradlew checkLicense`: the licences of each artifact's run-time dependencies, what applications receive, against `config/allowed-licences.json`, the licences `deny.toml` allows in itinera-rs;
- `./gradlew checkManifests`: the Kotlin and the Java manifests agree, as "Conformance" says;
- `./gradlew :build-logic:check`: the tests of the build's own code.

`./gradlew check dokkaGenerate publishAllPublicationsToPackageRepository` runs all of them, the tests on JDK 25. CI also runs `advisories`, [OSV-Scanner](https://google.github.io/osv-scanner/) over the lock files and the verification metadata through its GitHub action, `Kotlin conformance` and `Java conformance`. Each week, CI also runs the tests on the newest Kotlin, pre-releases included, and on the newest JDK.

The compile-fail tests are a test suite of their own, in `src/compileFailTest` of `itinera`, `itinera-kotlin` and `itinera-java`, so the compiler they need stays off the class path of the other tests, which run on the oldest Kotlin standard library itinera supports. In Kotlin, they compile snippets with [kctfork](https://github.com/ZacSweers/kotlin-compile-testing), at the Kotlin version that builds itinera, through the helpers in `src/compileFailTest/kotlin/org/itineradev/itinera/compiling` of `itinera` and `itinera-kotlin`; in Java, `itinera-java`'s suite compiles them with `javax.tools`, with `itinera-kotlin` on the class path. Each expects the message saying why the Kotlin compiler or javac refused the snippet. The twins are ordinary sources of the suite, in a file beside the test named with `Twins` for `Test`, such as `PolicyTypesTwins.kt`, so Gradle compiles them once with the suite, and only the refused snippets are compiled while the tests run.

## Dependencies

Every configuration of every project is locked, in `gradle.lockfile` beside its build script, and `settings-gradle.lockfile` holds only the version catalogue's configuration, since the toolchain resolver plugin is pinned by its version and verified, not locked. Every artifact Gradle fetches, plugins and tools included, is checked against its SHA-256 in `gradle/verification-metadata.xml`.

A change to a version, a dependency or a plugin rewrites both in the same commit:

1. `./gradlew :itinera:dependencies :itinera-kotlin:dependencies :itinera-java:dependencies :itinera-testing:dependencies :itinera-bom:dependencies :conformance:dependencies :conformance-java:dependencies :build-logic:dependencies dependencies --write-locks`, with every new project added to the list;
2. `GRADLE_USER_HOME=$(mktemp -d) ./gradlew --write-verification-metadata sha256 check dokkaGenerate publishAllPublicationsToPackageRepository --rerun-tasks`, which fetches every artifact the build uses and records it. The empty Gradle home matters: with a warm cache, Gradle never fetches the parent POMs and BOMs it already holds, so it never records them, and CI, which starts empty, then fails verification.

Read the diff of both files: every new entry is a dependency the change meant to add.

An advisory that `advisories` reports is fixed by raising the library. When a tool brings it, a constraint raises it where the tool resolves it: the Jackson BOM in build-logic, for the Dokka plugin, and the Jackson BOM with constraints on FreeMarker and jsoup in `dokkaHtmlGeneratorRuntime`, for Dokka's generator, which a Dokka plugin resolved in another of Dokka's configurations needs as well. Where no configuration can be named, as for the formatters Spotless resolves on its own, a component metadata rule raises it everywhere, as `RaiseGuava`, in the convention `itinera.raised-dependencies`, does for Guava. Only an advisory on a tool that runs on this repository's own sources alone, at a version the build cannot choose, such as the Kotlin Gradle plugin that Gradle's `kotlin-dsl` embeds, goes in `osv-scanner.toml`, with its reason and a date three months on, when it is checked again.

## Conformance

Two manifests, each of the shape the conformance cases' FORMAT.md describes: `conformance.json` for Kotlin and `conformance-java.json` for Java. Each pins the conformance cases, lists the proposals implemented and the capabilities claimed, and maps each scenario excluded as impossible to express to the test that proves it. A proposal is listed only by the pull request that completes it, and in both at once.

The two must agree, which `./gradlew checkManifests` checks: the same `cases`; the same `proposals`; Kotlin's `capabilities` are Java's with `async`, which only Kotlin claims; and under `impossible`, Java lists Kotlin's tags but `unexpected-result` and `non-string-journey-id`, which Java can write and never lists, each shared tag with the same scenarios, by `feature` and `scenario`. Each manifest names its own language's proof.

In CI, the `Kotlin conformance` job runs the `run-conformance` action of [itinera-dev/actions](https://github.com/itinera-dev/actions) with `language: kotlin`: it reads `conformance.json`, checks the excluded scenarios, starts the Kotlin runner, `./gradlew :conformance:run`, on the scenarios whose proposals are all listed, and uploads the report, `cucumber-kotlin.json` and `exclusions-kotlin.json`, as the artifact `conformance-report-kotlin`. The `Java conformance` job does the same with `manifest: conformance-java.json` and `language: java`: it reads `conformance-java.json`, starts the Java runner, `./gradlew :conformance-java:run`, and uploads `cucumber-java.json` and `exclusions-java.json` as the artifact `conformance-report-java`.

The Kotlin runner runs Cucumber-JVM twice, once for each column: the synchronous column, on `LocalExecutor`, and the asynchronous one, on `AsyncLocalExecutor`. A scenario tagged `@capability-async` runs only in the asynchronous column, and one tagged `@capability-sync` only in the synchronous one. The runner merges the two reports into one, each scenario tagged with its column, `@executor-local` or `@executor-async`, the second copy of a scenario that ran in both with a line of its own, and fails when a scenario, in either column, fails or uses a sentence the runner does not define. Its Given sentences fill a plain scenario model, in `conformance/src/main/kotlin/org/itineradev/itinera/conformance/model`. Its When sentences put the scenario's workflow together from that model with the builder of the column's mode, each step a step mock of `itinera-testing` playing the step's script, and run it on the column's executor, in `conformance/src/main/kotlin/org/itineradev/itinera/conformance/run`. Its Then sentences, in `conformance/src/main/kotlin/org/itineradev/itinera/conformance/sentences` with the Given ones, read the result, the events and what each scripted reporter received; the code that writes each event as a line of an event table, and compares lines, is in `conformance/src/main/kotlin/org/itineradev/itinera/conformance/trace`. A Then sentence that does not hold throws `Unmet`, a failure of itinera. A case that scripts what the runner cannot run until a later stage, such as an attempt that fails, throws `NotRunYet`, which says what it cannot run yet. A case the runner cannot read throws `ModelException`, an error in the case; so does a case the runner's language makes impossible, such as an asynchronous step handed to `LocalExecutor`, until the manifests exclude it. Its tests run it over the features under `conformance/src/test/resources/features`, where `journeys.feature` uses every sentence that a journey of this stage can satisfy, the few others, such as those reading a failure's reason, waiting for attempts that fail, and `failing.feature` shows that each kind of failure fails the run.

The Java runner, written wholly in Java against `itinera-java` and `itinera-testing`, runs Cucumber-JVM once, on `LocalExecutor`, since Java writes synchronous workflows only: the action never selects a scenario tagged `@capability-async`, which `conformance-java.json` does not claim, so its report has one line per scenario and no column tags. It fails as the Kotlin runner does, with `Unmet`, `NotRunYet` or `ModelException`. Its Given sentences fill a scenario model of its own, in `conformance-java/src/main/java/org/itineradev/itinera/conformance/model`. Its When and Then sentences, in `conformance-java/src/main/java/org/itineradev/itinera/conformance/sentences`, are the Kotlin runner's, written in Java: it puts the workflow together with the builder from `StepMock`s and runs it on `LocalExecutor`, in `conformance-java/src/main/java/org/itineradev/itinera/conformance/run`, and writes and compares the events as lines of an event table in `conformance-java/src/main/java/org/itineradev/itinera/conformance/trace`. Its tests run it over the features under `conformance-java/src/test/resources/features`, among them copies of the Kotlin runner's `journeys.feature` and `failing.feature`.

To run the cases yourself, check out [itinera-dev/conformance](https://github.com/itinera-dev/conformance) at the tag the manifests pin, then run a runner from this repository with:

- `ITINERA_CONFORMANCE_CASES` set to that checkout's `cases` directory;
- `ITINERA_CONFORMANCE_TAGS` set to a Cucumber tag expression selecting the scenarios to run;
- `ITINERA_CONFORMANCE_REPORT` set to the file the Cucumber JSON report is written to.

## Commit messages and the changelog

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/), and each commit that changes what an application meets carries its changelog entries as trailers, which [git-cliff](https://git-cliff.org/) gathers into a release's section of `CHANGELOG.md`. For example:

```text
feat(step)!: give an attempt a type of its own

An attempt's number was a bare Int, so nothing said that it
counted from 1 or named the first attempt.

Added: `Attempt` in `org.itineradev.itinera.step`, an attempt's number, counted from 1, with `isFirst`.
Changed: An attempt's number is an `Attempt` instead of an `Int` wherever it is public.
Refs #N
```

1. The subject is `type(scope): description`, or `type: description`. The types are `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `ci`, `build`, `chore` and `revert`, and the scope is a package's concept or an area, such as `step`, `value`, `java`, `release` or `conformance`. A `!` before the colon marks a change that breaks code written for the last release.
2. The body, after a blank line, says what changed and why.
3. The trailers close the message: one `Added:`, `Changed:`, `Fixed:` or `Removed:` line for each changelog entry, then `Refs #N` or `Closes #N`. An entry says what an application meets, by its package and name, naming the artifact when it is not the core, and for a breaking change what replaces what is gone. Changes to CI, the release workflow or the documentation have entries only when they change what is published or how.
4. A `feat`, `fix` or breaking commit with no entry, such as a fix to work that no release has shipped yet, says `Changelog: none`.

The required `commit messages` check runs the `commit-messages` action of [itinera-dev/actions](https://github.com/itinera-dev/actions) on every commit of a pull request other than merges. It refuses a subject of another form, a type outside the list, a body that does not follow a blank line, a `feat`, `fix` or breaking commit with neither entries nor `Changelog: none`, entries together with `Changelog: none`, and any entry that git-cliff would not read, would cut short or would read with more attached, such as an entry on several lines or a misspelt section. It does not check the wording. Before pushing, `PYTHONPATH=<actions> python3 <actions>/commit-messages/main.py origin/main..HEAD`, run in this repository with `<actions>` a checkout of itinera-dev/actions and Python 3.11 or later, checks the local commits the same way.

Pull requests merge with a merge commit, never squashed, because a squash folds every commit's trailers into one message. A commit's entries cannot change once it merges, so the release pull request reconciles entries that a later commit made untrue.

`cliff.toml` holds git-cliff's template. `git cliff --unreleased --tag v<version> --strip all` prints the section for `<version>` from the entries of the commits since the last release. The template is written for git-cliff 2.14.2, which installs with `pip install git-cliff==2.14.2`.

## Releasing

Every artifact is published to [Maven Central](https://central.sonatype.com/) under the group `org.itinera-dev`, at one version. A maintainer requests a release by pushing, on `main`, an annotated tag `v` followed by its version, such as `v0.1.0-beta.1`, with the message `itinera-j <version>`, after the pull request that sets the version and adds its section to `CHANGELOG.md` has merged. The release workflow, `.github/workflows/release.yml`, then runs four jobs, each waiting for the one before:

1. **release gate**: the `release-gate` action of itinera-dev/actions refuses the tag unless it matches the version in `gradle.properties`, the commit is on `main`, `CHANGELOG.md` has a section for the version, and the manifests list every tier 1 proposal of the pinned cases, except one whose every case needs a capability the manifest does not claim. The action reads `Cargo.toml` and `package.json` today; it learns to read `gradle.properties` and the second manifest before the first release;
2. **CI**: the whole of `.github/workflows/ci.yml` runs again on the tagged commit, since its run on `main` after a merge blocks nothing, and its conformance jobs keep the Kotlin and the Java reports;
3. **Maven Central**: in the `maven-central` environment, whose deployment rule lets only tags `v*` use it, `./gradlew publishAndReleaseToMavenCentral` signs every artifact, uploads them to the Central Portal as one deployment and releases it, with the portal's user token and the signing key taken from the environment's secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`;
4. **GitHub release**: the `create-release` action of itinera-dev/actions creates the release with the notes of the version's changelog section, as a pre-release for a version such as `0.1.0-beta.1`, and attaches both conformance reports of the CI run: the Kotlin one, `cucumber-kotlin.json` and `exclusions-kotlin.json`, and the Java one, `cucumber-java.json` and `exclusions-java.json`.

A version published to Maven Central can never be deleted or replaced, which is why the gate and CI come first. Before pushing the tag, the maintainer runs `./gradlew check dokkaGenerate publishAllPublicationsToPackageRepository` at the commit to be tagged.

Each artifact's javadoc jar, which Central requires and Dokka makes, is the artifact's API reference, which [javadoc.io](https://javadoc.io/) serves for every release with no work of ours.

The release gate, the GitHub release and the conformance run are actions of [itinera-dev/actions](https://github.com/itinera-dev/actions), shared with every language, which this repository uses at `v1`.
