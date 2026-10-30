# Agents' manual

How to work in itinera-j, the JVM implementation of [Itinera](https://github.com/itinera-dev/spec): a core written in Kotlin, a Kotlin layer with both execution modes, and a Java layer for synchronous workflows. This file is self-contained; read it before changing anything.

## Where behaviour comes from

- Behaviour comes only from the specification in [itinera-dev/spec](https://github.com/itinera-dev/spec), and the conformance cases in [itinera-dev/conformance](https://github.com/itinera-dev/conformance) check it.
- If the specification has a gap or a contradiction, stop and tell the maintainer. Never decide it in Kotlin or in Java.
- How the JVM implements tier 1 is decided in [docs/tier-1-plan.md](docs/tier-1-plan.md). What concerns a single proposal goes in its tech spec, `docs/specs/NNNN-short-name.md`. Where the plan and the specification disagree, the specification is right and the plan is corrected.
- `docs/architecture.md` draws how the implementation is put together and how it runs a journey; it starts in stage 3, and each later stage adds what it builds. A change that makes any of its diagrams, lists, tables or "Where it lives" lines untrue updates it in the same pull request, and a later tier adds its own section once its stages land. Where the document and the code disagree, the document is corrected.

## Stages

The plan is implemented in stages, tracked in [#1](https://github.com/itinera-dev/itinera-j/issues/1):

0. Bootstrap: the Gradle build and its modules, formatting, static analysis, JUnit, CI, the commit convention and git-cliff, the README, the licences, this manual, and the manifests `conformance.json` for Kotlin and `conformance-java.json` for Java.
1. Values and events: the value check, the private copy at capture and a fresh copy for each receiver, typed reads by runtime type, with the data class walk in `itinera-kotlin`, `TypeToken`, the data bag and its `Read`, `Event` and what it carries, reporters, dispatchers and their factories in both modes, `DefaultDispatcher`, global metadata on dispatchers.
2. The Kotlin runner's skeleton: Cucumber-JVM, the environment variables, the scenario model, the two columns and the report, the recording dispatcher factory and the recording reporter, the manifest inputs of `run-conformance`, and the conformance change for two languages in one repository.
3. Minimal executor: the engine and both executors, both builders and `WorkflowDescriptor.of` for a workflow of one step that succeeds, both readers, `@Workflow` and `@WithStep`, the instance and journey IDs, reporters, the result and refusals; test support's step mock; `itinera-java`, the Java runner, its scenario model and report, and the Java usage tests of the core.
4. Declarations and admission: every interface, annotation and builder method, so everything is declared and checked: steps, policies, roles, input adapters, service providers, rescue steps and each part's mode; every violation, also checked by `run` for hand-written descriptors; the listing.
5. Steps: resolving requests, building per attempt, input adapters, service providers of both scopes lending what steps request, with their requests, data and implementation names, outcomes, contributions, handles in both modes and the executor's signal, steps setting global metadata.
6. The scan and its decisions: statuses, retries, `retry on abnormal termination`, aborts, the result and `output`.
7. Policies, hooks and roles.
8. Services: providers' reporters, their events and the global metadata they set.
9. Rescue steps and unexpected results; tier 1 complete.
10. Test support: the rest of `itinera-testing`, both runners' test support sentences, `testing` claimed in both manifests.
11. Later cases: any newer cases candidate.
12. Compile-time checks: `itinera-checker`, a KSP processor and a javac plugin over shared rules.
13. Release preparation: the API reference, the overview, examples, the release workflow, ready for the first release, published only after the maintainer merges the stack.
14. The book: the Kotlin edition of [itinera-dev/book](https://github.com/itinera-dev/book), as a stack of its own.

A proposal is added to `proposals` in both manifests only by the pull request that completes it, which says `Closes #N` for its implementation issue, and only once all its scenarios pass in both runners and every column. A proposal is listed only once its cases are at the tag both manifests pin. Proofs that a rule is impossible to express land with their feature, together with their entries under `impossible`.

## Kotlin and Java

- Kotlin is the primary implementation, and Java is a thin layer over the same core, never a second engine. Java writes synchronous workflows only.
- From stage 3, the two stay compatible, as section 15 of the plan says. Both manifests pin the same cases and list the same proposals; Kotlin's capabilities are Java's plus `async`; Java excludes as impossible what Kotlin excludes, by the same scenarios, but for `unexpected-result` and `non-string-journey-id`, which Java never lists. The `manifests agree` check holds this. Both runners are required on every pull request. A feature lands in both languages in the same pull request: its Kotlin API and, where Java has it, its Java API, its sentences in both runners, and its fixtures in both. A release ships both or neither.
- The core's public API keeps the five rules of the plan's "What keeps the core reachable from Java": names are small final classes, not value classes; the synchronous builder takes `fun interface`s, not Kotlin function types; factories are static from Java, through `@JvmStatic` and `@JvmOverloads`; events and the result are `@JvmRecord` data classes under sealed interfaces; and every reified call has a form taking a `TypeToken`. From stage 3, the Java usage tests check them.
- Every `suspend` function carries `@JvmSynthetic`. Every asynchronous type lives in `itinera-kotlin`.

## Code

- Formatting is ktfmt in its Kotlin style for Kotlin, and google-java-format for Java, through Spotless, with no other configuration. `./gradlew spotlessApply` formats everything.
- Names use the specification's vocabulary exactly, in camelCase as proposal 0111 shows, such as `keepDefault`, `fromDataBag` and `isFirst`, and types in PascalCase.
- Kotlin's explicit API mode is on, and every warning, Kotlin's and javac's, is an error. detekt runs on its defaults with the few changes in `config/detekt/detekt.yml`.
- While a journey runs, no throw escapes `run`: every failure there becomes the journey's result, an abort or a failure, as the specification says. `java.lang.Error` and its subclasses are never caught, and reach whoever called `run`; every other `Throwable` from custom code is caught. Before a journey, a refusal throws `Refusal`, and while a workflow is described or built, a misuse that types cannot rule out throws, with a message saying what was wrong, such as an empty step name.
- Reflection lives only in the annotation readers, the value walk and the lending of roles, each behind an interface, and in `TypeToken`'s one read of its superclass. The engine and the rest of the builder never use it otherwise. Outside them, the builders, `Requests.role` and the asynchronous executor, when it is made or admits an instance, may check what a class is marked or made as, with `isAnnotationPresent`, `isInterface` and `isHidden`, which read none of its members, to keep a rule that types cannot hold, such as a role being an interface marked `@Role`, or an asynchronous part being written in Kotlin. The engine may read the class of an instance a service provider lends, its name and `isHidden`, to name the implementation that `service_provided` carries.
- Nothing in the core, `itinera`, uses kotlin-reflect or kotlinx.coroutines; they belong to `itinera-kotlin`. The core brings Kotlin's standard library only.
- Reuse before writing. Look first at the JDK and Kotlin's standard library, then at libraries the build already depends on, then at well-known libraries. Write it ourselves only when none fits, or when a library would replace only a couple of lines. Judge a library by what it will replace across the project, not only by its first use. Every runtime dependency has a stated reason in the tech spec that adds it.
- Types hold every constraint of the specification and the plan that Kotlin can express: name classes such as `StepName`, an `Attempt` counted from 1 with `isFirst`, sealed interfaces for closed sets of cases, a mode in the instance's type, `val` and read-only collections everywhere. A documentation comment stating a constraint that the type could enforce means the type is wrong.
- Whatever the types can refuse does not compile, and a compile-fail test shows it, beside a twin that compiles: in Kotlin with kotlin-compile-testing, in its fork `dev.zacsweers.kctfork`, and in Java with `javax.tools`. Compile-fail tests live in the module's `src/compileFailTest`, a test suite of their own, in the package of the code whose types refuse the snippet. The twins are ordinary sources of the suite, in a file named after the test with `Twins` for `Test`, such as `PolicyTypesTwins.kt`, so Gradle compiles them once with the suite and only the refused snippets go through a compiler while the tests run. A rule that types cannot hold is checked when the workflow is described or built, or at run time.
- Closed sets of names, such as hook kinds and statuses, are enums. Data that varies by case is a sealed interface, matched with an exhaustive `when` that has no `else`.
- Annotations on plain classes implementing itinera's interfaces are the main way an application declares. The public builder is the core they are read into, and the conformance runners use it.
- Annotations take the names of Rust's macros, since they are the API applications write: `@Workflow`, `@Step`, `@RescueStep`, `@StepPolicy`, `@WorkflowPolicy`, `@Role` and the others. An interface whose name such an annotation needs carries the `Itinera` prefix, such as `ItineraWorkflow`, `ItineraAsyncWorkflow`, `ItineraStep`, `ItineraRescueStep`, `ItineraStepPolicy` and `ItineraWorkflowPolicy`; every other interface keeps its plain name, such as `Reporter` or `Event`. A role interface is marked `@Role`, never by extending a marker interface.
- Packages follow concepts. Every package is under `org.itineradev.itinera`, named with the specification's vocabulary, such as `journey`, `step` or `event`, and each type lives in the package of the concept it belongs to, never where it happens to be used first: `JourneyId` is in `journey`, though events use it before the result does. What an artifact adds to a concept lives in a subpackage of that concept's package named for the artifact's face, such as `executor.async` or `value.kotlin`, since the JVM's module system refuses a package split across two artifacts.
- Each artifact is a Java module, `org.itineradev.itinera` for `itinera`, named after the artifact, whose descriptor, `src/main/java/module-info.java`, exports its public packages and not the engine's. What another itinera artifact calls in the engine is exported only to that artifact's module.
- Everything is `internal` or `private` unless it must be public. Public types that may grow keep room to, such as a sealed interface's members or a class with a private constructor.
- A collection operation that builds a result reads as a sentence of names, such as `.map(::toNewFormat).filter(::onlyOven).maxWith(lesser)`, and the details live in the functions it names. Each step is a function reference, or a lambda whose whole body is one call, one method call or one property read, such as `{ toNewFormat(it, unit) }`, `{ it.name }` or `{ step -> step.actions }`; taking one part of a pair, as in `{ (_, script) -> script }`, counts as a property read. Its arguments are names, literals, properties or one conversion of them. A lambda that does more, comparing, negating, matching, computing, building a value or chaining calls, becomes a named function.
- Iterating to get a result uses collection operations, not a `for` loop. A loop that searches, stopping with `return` or `break` once an item matches, is `first`, `firstOrNull`, `indexOfFirst`, `any`, `all` or `none`. One that builds a new value is `map`, `filter`, `mapNotNull`, `flatMap`, `fold`, `associate` or `count`. A `for` loop stays only when its body acts on each item, such as filling an existing model, delivering an event or asserting in a test, and it may stop early when that work fails.
- A file past 200 lines holds one thing. A file's length counts its code and its documentation; test files are not counted. Past 200 lines, a file holding two things, related or not, keeps one of them and moves each of the others to a file of its own. Only a file whose lines all belong to one class, interface or object, with its documentation, its own members and the private helpers only it uses, may stay longer.
- Unit tests live beside the code they check, in the same package of the module's `src/test`, with JUnit Jupiter. A test goes with the code that decides what it checks, even when it runs a whole journey to check it.
- Every line does something. Code, tests and samples hold no line that changes nothing, such as a cast to the type a value already has.
- Every public item has KDoc in British spelling, describing its behaviour in its own words, with a sample that compiles and runs: a function in the tests, pulled in with Dokka's `@sample`, which a test runs, so no sample can rot. Samples use the packages applications import.
- Test names state the rule as a sentence, in backticks, for example ``fun `a failed attempt's contributions are never committed`()``.
- A rule checked on several inputs is one `@ParameterizedTest` with a named case per input, never a run of assertions or a loop over a table. The test's name states the rule and each case's name says what its input is. A test over every value of a closed set, such as every hook kind, takes the set itself, through `@EnumSource` or the enum's `entries`, so that a new value is checked without a new case.
- A chain that three or more tests of one class start the same way, from the same constructor or helper, becomes a helper of those tests, named for what it makes, such as `shopWorkflow()`. When the tests go on differently, the helper makes only the part they share, so each test adds only what varies.

## Comments

- As few as possible. A comment explains a non-obvious reason, only when the code cannot.
- Code and comments never refer to specification sections, issues, pull requests or other documents. Git keeps that history, and the tech specs map rules to code.
- No `TODO` comments, and no Kotlin `TODO()`, which throws an `Error`.

## Running the checks

Each of these is a required CI check, and every one must pass before pushing, except as the paragraph after the list says for the compile-fail tests:

- `./gradlew spotlessCheck`
- `./gradlew detekt`
- `./gradlew assemble`
- `./gradlew test`, which CI runs on JDK 17, 21 and 25, each with `-Pitinera.test-jdk=N`
- `./gradlew compileFailTest`
- `./gradlew checkKotlinAbi`
- `./gradlew dokkaGenerate`
- `./gradlew publishAllPublicationsToPackageRepository`
- `./gradlew examples`
- `./gradlew checkLicense`
- `./gradlew checkManifests`
- `./gradlew :build-logic:check`

`./gradlew check dokkaGenerate examples` runs all of them, the tests on JDK 25. The compile-fail tests are the slowest, so before pushing they are run only when a change touches a public signature, a type that refuses something at compile time, the compile-fail suites or what compiles them; otherwise add `-x compileFailTest`. CI runs them on every pull request. A change to the public API also runs `./gradlew updateKotlinAbi` and commits the dumps under each artifact's `api/`. A change to a dependency or a plugin rewrites the locks and the verification metadata, as docs/contributing.md "Dependencies" says.

CI also runs the `advisories` check, OSV-Scanner over the lock files and the verification metadata, which has no Gradle task, `Kotlin conformance` and `Java conformance`.

The `commit messages` check is required as well. Before pushing, run it on the commits not pushed yet, as docs/contributing.md "Commit messages and the changelog" says, since a pushed commit that has a layer of a stack above it cannot be reworded without rebasing that layer.

## Running the conformance runners

The Kotlin runner, from stage 2, is the project `conformance`, run with `./gradlew :conformance:run`; the Java runner, from stage 3, is the project `conformance-java`, run with `./gradlew :conformance-java:run`. Each reads:

- `ITINERA_CONFORMANCE_CASES`, the `cases` directory of a checkout of itinera-dev/conformance at the tag the manifests pin;
- `ITINERA_CONFORMANCE_TAGS`, a Cucumber tag expression, for example `@proposal-0002 and not @non-value`;
- `ITINERA_CONFORMANCE_REPORT`, the file the Cucumber JSON report is written to.

In CI the `run-conformance` action sets these from each manifest.

## Pull requests and stacks

- Tier 1 is one long stack of pull requests, linked through GitHub's stacks API: one or more pull requests per project thread, each based on the layer below, the threads running one after another. No layer merges until the maintainer merges the stack. Every layer passes all of `main`'s checks.
- `gh api` working is a hard requirement of every session that adds to the stack: check it before any other work, and stop if it fails.
- Every pull request names an open issue in this repository: `Refs #N` when it contributes, `Closes #N` when it finishes. Work that belongs to no proposal refers to #1, or to an issue of its own.
- Public API of a proposal not yet listed in both manifests, or of one the specification has not accepted yet, carries the opt-in marker `@ExperimentalItineraApi`, so Kotlin refuses to compile a use of it without an opt-in. So does a listed proposal's API while it holds a stand-in that a later stage replaces, or names API of a proposal not yet listed.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/). The subject is `type(scope): description`, or `type: description`, with the description in the imperative and starting in lower case, such as `feat(step)!: give an attempt a type of its own`. The type is one of `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `ci`, `build`, `chore` and `revert`. The scope, when there is one, is a package's concept or an area, such as `step`, `value`, `java`, `release` or `conformance`. A `!` before the colon marks a change that breaks code written for the last release. The body says what changed and why, in plain prose.
- A commit that changes what an application meets, compared with the last release, carries its changelog entries as trailers, in the paragraphs that close the message, after the body: one `Added:`, `Changed:`, `Fixed:` or `Removed:` line per entry, before `Refs #N` or `Closes #N`. An entry says what an application meets, by its artifact and its type's name, such as `Read.present` in `org.itineradev.itinera.journey`, and for a breaking change what replaces what is gone. A `feat`, `fix` or breaking commit that changes nothing an application meets, such as a fix to work no release has shipped yet, says `Changelog: none` instead. The `commit messages` check refuses a message that breaks this form, and git-cliff builds each release's changelog section from the entries, as `docs/contributing.md` says.
- Pull requests merge with a merge commit, never squashed, since a squash folds every commit's trailers into one message.
- Pull request descriptions say what changed and why, in plain prose.
- Every change an agent makes is also reviewed by a separate reviewer agent. A new pull request is reviewed before it is opened, since that is the version the maintainer reads first. A later change to an open pull request is pushed as soon as the checks under "Running the checks" that it calls for pass, and is reviewed while CI runs; an accepted finding becomes a further push, reviewed in the same way. A review passes when it has no accepted finding left.

## Writing

- Everything is written for a screen reader: headings, lists, prose and simple tables.
- Never ASCII-art diagrams, arrows drawn with characters or box-drawing trees, in replies, documents or code. Diagrams are Mermaid, with `accTitle` and `accDescr`, and text saying everything they show.

## Working with the maintainer

- Start every reply with a heading.
- When a design decision is open, present one decision at a time, with a recommendation, and wait for the answer.
- Tell the maintainer a pull request is ready for review only when both the review and the CI of its latest push have passed, as soon as the later of the two does.
