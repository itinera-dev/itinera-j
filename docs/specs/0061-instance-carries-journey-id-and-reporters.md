# 0061: The workflow instance carries its journey ID and its reporters

Tech spec for [proposal 0061](https://github.com/itinera-dev/spec/blob/main/proposals/0061-instance-carries-journey-id-and-reporters.md), implemented in [#22](https://github.com/itinera-dev/itinera-j/issues/22). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 10 above all.

Status: started. Stage 3 added the instance, its builders in both modes, the journey ID and the reporters.

## API

Until 61 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`WorkflowInstance<M>`**, in `org.itineradev.itinera.instance`, holds the descriptor and the workflow object side by side, the journey ID, the reporters, the initial data and the initial global metadata. `claim()` is `true` only the first time it is called, so the executor refuses an instance that ran, with `Refused.InstanceRan`. The interface is public, so a hand-written instance runs too.
- **`InstanceBuilder`** creates the instance of a synchronous workflow. `instance()` on a class implementing `ItineraWorkflow` starts one; `InstanceBuilder.of(descriptor, workflow)` starts one for a descriptor the builder made, and Kotlin also writes it `descriptor.instance(workflow)`. The plan writes `descriptor.instance(orders)` as a method; the descriptor interface is shared by both modes, so the method became a static factory, which Java reaches, and an extension, `@JvmSynthetic`, which only Kotlin sees.
- **`AsyncInstanceBuilder`**, in `org.itineradev.itinera.instance.async` in `itinera-kotlin`, does the same for an asynchronous workflow, started by `instance()` on a class implementing `ItineraAsyncWorkflow`, by `AsyncInstanceBuilder.of(descriptor, workflow)` or by `descriptor.instance(workflow)`. It takes `AsyncReporter`s, and `reporterFor` takes an `AsyncReporterFactory`, in `org.itineradev.itinera.report.async`, an ordinary function as the plan's section 10 asks, since `create()` is one in both modes. Both builders fill the engine's `Draft`, so they create instances the same way.
- **What it takes:** `data(key, value)`, `initialData(map)`, `reporter(reporter)`, `reporterFor(factory)` with a `ReporterFactory` given the journey ID, and `reportersGlobalMetadata(key, value)`. `reporterFor` has a name of its own: `Reporter` and `ReporterFactory` are both single-method interfaces taking one argument, so Kotlin and Java could not tell a lambda for one from a lambda for the other. A key given twice keeps the later value, in the first one's place. Data classes and records given to `initialData` come with the result's `output` in stage 6.
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
- `itinera-kotlin/src/compileFailTest/kotlin/org/itineradev/itinera/instance/async/InstanceModeTypesTest.kt`: compile-fail tests, each beside a twin that compiles: each instance builder takes only the reporters and reporter factories of its own mode.

## Done when

61 is listed in both manifests once every scenario tagged `@proposal-0061` passes in both runners.
