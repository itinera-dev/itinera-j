# 0061: The workflow instance carries its journey ID and its reporters

Tech spec for [proposal 0061](https://github.com/itinera-dev/spec/blob/main/proposals/0061-instance-carries-journey-id-and-reporters.md), implemented in [#22](https://github.com/itinera-dev/itinera-j/issues/22). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 10 above all.

Status: done. Stage 3 added the instance, its builders in both modes, the journey ID and the reporters. Stage 6 gives both runners the sentence that checks the journey ID a listed reporter was made with, and lists 61 in both manifests.

## API

61 is listed in both manifests, and from stage 7 no public item below carries `@ExperimentalItineraApi`, but for the global metadata of [0102](0102-global-metadata.md), not listed yet: `reportersGlobalMetadata` on both instance builders and `WorkflowInstance.initialGlobalMetadata`.

- **`WorkflowInstance<M>`**, in `org.itineradev.itinera.instance`, holds the descriptor and the workflow object side by side, the journey ID, the reporters, the initial data and the initial global metadata. `claim()` is `true` only the first time it is called, so the executor refuses an instance that ran, with `Refused.InstanceRan`. The interface is public, so a hand-written instance runs too.
- **`InstanceBuilder`** creates the instance of a synchronous workflow. `instance()` on a class implementing `ItineraWorkflow` starts one; `InstanceBuilder.of(descriptor, workflow)` starts one for a descriptor the builder made, and Kotlin also writes it `descriptor.instance(workflow)`. The plan writes `descriptor.instance(orders)` as a method; the descriptor interface is shared by both modes, so the method became a static factory, which Java reaches, and an extension, `@JvmSynthetic`, which only Kotlin sees.
- **`AsyncInstanceBuilder`**, in `org.itineradev.itinera.instance.async` in `itinera-kotlin`, does the same for an asynchronous workflow, started by `instance()` on a class implementing `ItineraAsyncWorkflow`, by `AsyncInstanceBuilder.of(descriptor, workflow)` or by `descriptor.instance(workflow)`. It takes `AsyncReporter`s, and `reporterFor` takes an `AsyncReporterFactory`, in `org.itineradev.itinera.report.async`, an ordinary function as the plan's section 10 asks, since `create()` is one in both modes. Both builders fill the engine's `Draft`, so they create instances the same way.
- **What it takes:** `data(key, value)`, `initialData(map)`, `initialData(data)` with a record or data class, `reporter(reporter)`, `reporterFor(factory)` with a `ReporterFactory` given the journey ID, and `reportersGlobalMetadata(key, value)`. `reporterFor` has a name of its own: `Reporter` and `ReporterFactory` are both single-method interfaces taking one argument, so Kotlin and Java could not tell a lambda for one from a lambda for the other. A key given twice keeps the later value, in the first one's place. A record or data class given to `initialData` puts each component under its key, as [0065](0065-business-result.md) describes with the result's `output`.
- **`create()`** captures the initial data and the initial global metadata, then produces the journey ID, then makes the reporters, as chapter 5, 5.1 orders them. Values that are not values are recorded, not thrown, so `run` refuses them with every key at once.

## The journey ID

The workflow's `JourneyIdGenerator`, called with the workflow object and a data bag of the captured initial data, or a UUID version 4 without one. A generator that throws, or returns `null` or an empty string, which only Java code can, makes `create()` throw `IllegalStateException`, carrying what it threw and saying what it returned, so no journey starts and no event is emitted. A reporter factory that throws or returns `null` does the same.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DraftTest.kt`: a UUID version 4 by default, the generator's reads, a generator that throws or returns an empty ID, reporter factories given the ID, a reporter factory that throws, and a key given twice.
- `itinera/src/test/kotlin/org/itineradev/itinera/instance/InstanceBuilderTest.kt`: what the instance holds, and that it is claimed once.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: an ID generator or a reporter factory returning `null` from Java.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/instance/async/AsyncInstanceBuilderTest.kt`: what an asynchronous instance holds, and a reporter factory given the journey ID.
- `itinera-kotlin/src/compileFailTest/kotlin/org/itineradev/itinera/instance/async/InstanceModeTypesTest.kt`: compile-fail tests, each beside a twin that compiles in the matching `Twins` file: each instance builder takes only the reporters and reporter factories of its own mode.

## Conformance

Of the three scenarios tagged `@proposal-0061`, "A reporter is made with the journey ID" runs once 61 is listed, and passes in both runners and every column: each runner's recording reporter keeps the journey ID its factory was given, and the sentence "the reporter ... was made with the journey ID ..." reads it. The two of `journey-id-values.feature` are also tagged `@proposal-0122`, so they run once 122 is listed, in stage 9. "An ID generator that returns an empty string fails the instance" already passes in both runners; "An ID generator that returns something that is not a string fails the instance" is impossible in Kotlin, as the Kotlin manifest lists, and the Java runner gains its sentence in stage 9.

## Done when

61 is listed in both manifests by the pull request of stage 6 that adds the runners' sentence for a reporter made with the journey ID, which says `Closes #22`.
