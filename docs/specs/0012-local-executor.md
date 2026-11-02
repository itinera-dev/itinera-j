# 0012: The local executor

Tech spec for [proposal 0012](https://github.com/itinera-dev/spec/blob/main/proposals/0012-local-executor.md), implemented in [#8](https://github.com/itinera-dev/itinera-j/issues/8). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 11 above all.

Status: done. Stage 3 added `LocalExecutor`, `AsyncLocalExecutor` and the engine for a workflow whose steps succeed or terminate abnormally. Stage 5 added outcomes and the decision after a failure. Stage 6 lists `mode-not-accepted` as impossible in both manifests, with Java's proof, and lists 12 in both manifests.

## API

12 is listed in both manifests, and from stage 7 no public item below carries `@ExperimentalItineraApi`, but for `Refused.GlobalMetadataNotSet`, which belongs to [0102](0102-global-metadata.md), not listed yet. Until then, each kept the marker, as AGENTS.md allows, since it named API of a proposal not listed yet.

- **`LocalExecutor`**, in `org.itineradev.itinera.executor`, is made with a `DispatcherFactory`, or with `DefaultDispatcherFactory` when none is given; Java reaches both constructors through `@JvmOverloads`. Its `run(instance: WorkflowInstance<Sync>)` runs the journey on the caller's thread and returns its `JourneyResult`, or throws `Refusal` before any journey, which Java sees as checked through `@Throws`.
- **`AsyncLocalExecutor`**, in `org.itineradev.itinera.executor.async` in `itinera-kotlin`, is made with an `AsyncDispatcherFactory`, or with `AsyncDefaultDispatcherFactory` when none is given. Its `suspend fun run(instance: WorkflowInstance<Async>)`, `@JvmSynthetic`, runs the journey in the caller's coroutine, on whatever dispatcher it uses, and returns its `JourneyResult`, or throws `Refusal` before any journey. It never spawns, sleeps, sets timers or chooses a thread.
- **One journey at a time.** A call to `run` while the executor runs a journey, even from inside that journey, as a reporter could, is refused with `Refused.JourneyRunning`, before the instance is claimed, so the instance can still run later.

## How it is built

- **One engine.** `Engine`, in the engine package, is written once as `suspend` functions and calls the parts of a workflow through `Parts`, one implementation per mode: `SyncParts` in the core, and the asynchronous one in `itinera-kotlin`. `LocalExecutor` starts the engine with `driven`, which calls `startCoroutine` with an empty context and returns once the coroutine completes. A synchronous journey never suspends, so a suspension is a bug in itinera, and `driven` throws an `AssertionError`, as section 11 of the plan says.
- **The scan.** Each step is attempted in order. An attempt is built with the step's factory, then run. An attempt that returns `Outcome.success()` ends with `step_succeeded`, one that skips with `step_skipped`, and the journey goes on. One that fails ends with `step_failed`, and one that throws with `step_abnormal_termination`, and the pure functions `afterFailure` and `afterAbnormalTermination` decide what follows: a retry while the retry budget lasts, and for an abnormal termination while `retry on abnormal termination` is on, otherwise `step_given_up`, with the cause `failure` or `abnormal termination` for a budget of 0, or for an abnormal termination with retries off, and `retries exhausted` once a budget greater than 0 is spent. A step given up fails the journey, and no step after it runs; an abort ends the journey with `journey_aborted`, and no step after it runs either. From stage 7, a hook can also end the journey early with `FinishWorkflow` or `FailWorkflow`, as [0010](https://github.com/itinera-dev/spec/blob/main/proposals/0010-hooks-lifecycles-and-roles.md) says, and from stage 9 a rescue step takes over a failed attempt instead of the step being given up.
- **Two sets of parts.** `SyncParts` and `itinera-kotlin`'s `AsyncParts` each accept only the step factories and reporters of their own mode, make the journey's dispatcher with the executor's factory, build a step's attempt, and wrap each reporter in the engine's guard. `AsyncParts` fills `ensureActive()` with the coroutine context's own `ensureActive()`; `SyncParts` does nothing there.
- **Cancellation.** When custom code throws, `calling` asks `ensureActive()` before the engine decides anything, so a `CancellationException` abandons the journey, with no further event, only when the coroutine running `AsyncLocalExecutor.run` was cancelled. Any other `CancellationException`, such as a step's own `withTimeout`, is that code failing: an abnormal termination from a step, `reporter failed` from a reporter. Once `journey_aborted` is delivered, where every failure is ignored, the engine asks again, so a journey cancelled meanwhile is abandoned rather than returning its abort ([0081](0081-reporter-failure-stops-delivery.md) point 5).
- **The clock.** The engine stamps each event with `Instant.now(clock)`, the system's UTC clock in `LocalExecutor`, and a fixed one in the tests.
- **What is caught.** Every call into custom code goes through `calling`, which catches everything but an `Error`. An `Error` reaches whoever called `run`.

## Dependencies

- **kotlinx-coroutines-core**, in `itinera-kotlin` alone, for `ensureActive()` on the caller's coroutine context, since the standard library has coroutines but no `Job`. The core stays free of it, as the plan's "Kotlin and Java" rules ask. Apache-2.0.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: the events of a journey whose steps succeed, numbering and stamping, an abnormal termination with and without retries, retries exhausted, a step that cannot be built, no step running after a step is given up or after an abort, and an `Error` that is never caught.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DecisionTest.kt`: what follows an abnormal termination, for each budget and setting.
- `itinera/src/test/kotlin/org/itineradev/itinera/executor/LocalExecutorTest.kt`: a journey refused while one runs.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a Java workflow built with lambdas runs on `LocalExecutor`.
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/executor/ExecutorTypesTest.kt`: a compile-fail test, beside a twin that compiles in the matching `Twins` file: `LocalExecutor` runs only a synchronous instance.
- `itinera-kotlin/src/compileFailTest/kotlin/org/itineradev/itinera/executor/async/AsyncExecutorTypesTest.kt`: compile-fail tests, each beside a twin that compiles in the matching `Twins` file: each executor takes only the instances and dispatcher factories of its own mode.
- `itinera-java/src/compileFailTest/java/org/itineradev/itinera/java/AsyncOutOfReachTest.java`: javac, through `javax.tools` and with `itinera-kotlin` on the class path, refuses a call of `AsyncLocalExecutor.run`, whose twin in `AsyncOutOfReachTwins.java` calls `LocalExecutor.run`.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncLocalExecutorTest.kt`: an asynchronous journey awaits each step, a journey refused while one runs, a cancelled coroutine abandons its journey with no further event, a step's own timeout is an abnormal termination, a reporter's own `CancellationException` is `reporter failed`, a cancellation while `journey_aborted` is delivered abandons the journey, and an instance of the other mode is refused with `mode not accepted` for the workflow, its step and its reporter.
- `itinera-java/src/compileFailTest/java/org/itineradev/itinera/workflow/JavaModeTypesTest.java`: javac refuses giving the synchronous builder an asynchronous step, input adapter or service provider, whose twins in `JavaModeTypesTwins.java` give it synchronous ones.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/async/AsyncWorkflowsTest.kt`: a Java step in an asynchronous workflow, `JavaParcels.java`, is refused with `mode not accepted` when the workflow is described, since Java cannot write a `suspend` run.

## Conformance

Both manifests list `mode-not-accepted` under `impossible`, for its three scenarios: a synchronous-only executor refusing an asynchronous step, an asynchronous input adapter and an asynchronous provider. Each instance carries its mode in its type, and each builder takes only the parts of its own mode, so none of them compiles. The Kotlin manifest names the proofs `BuilderTypesTest`, `AdapterModeTypesTest` and `ProviderModeTypesTest`, and the Java manifest `JavaModeTypesTest`. Java has two more proofs, as the plan's "Rules made impossible" asks: javac refuses a call of `AsyncLocalExecutor.run` (`AsyncOutOfReachTest`), and the annotated form, a Java class implementing `ItineraAsyncWorkflow` with a synchronous step, is refused when described (`AsyncWorkflowsTest`), as a Kotlin step without `suspend` is in the same test. An unchecked cast can still hide a mode, which `run` refuses with `mode not accepted`, as `AsyncLocalExecutorTest` and `StartTest` show. The other four scenarios tagged `@proposal-0012` pass in both runners and every column.

## Done when

12 is listed in both manifests by the pull request of stage 6 that lists `mode-not-accepted` as impossible, which says `Closes #8`.
