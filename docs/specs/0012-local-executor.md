# 0012: The local executor

Tech spec for [proposal 0012](https://github.com/itinera-dev/spec/blob/main/proposals/0012-local-executor.md), implemented in [#8](https://github.com/itinera-dev/itinera-j/issues/8). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 11 above all.

Status: started. Stage 3 added `LocalExecutor` and the engine for a workflow whose steps succeed or terminate abnormally. Stage 6 completes the scan and its decisions.

## API

Until 12 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`LocalExecutor`**, in `org.itineradev.itinera.executor`, is made with a `DispatcherFactory`, or with `DefaultDispatcherFactory` when none is given; Java reaches both constructors through `@JvmOverloads`. Its `run(instance: WorkflowInstance<Sync>)` runs the journey on the caller's thread and returns its `JourneyResult`, or throws `Refusal` before any journey, which Java sees as checked through `@Throws`.
- **One journey at a time.** A call to `run` while the executor runs a journey, even from inside that journey, as a reporter could, is refused with `Refused.JourneyRunning`, before the instance is claimed, so the instance can still run later.

## How it is built

- **One engine.** `Engine`, in the engine package, is written once as `suspend` functions and calls the parts of a workflow through `Parts`, one implementation per mode: `SyncParts` in the core, and the asynchronous one in `itinera-kotlin`. `LocalExecutor` starts the engine with `driven`, which calls `startCoroutine` with an empty context and returns once the coroutine completes. A synchronous journey never suspends, so a suspension is a bug in itinera, and `driven` throws an `AssertionError`, as section 11 of the plan says.
- **The scan, for now.** Each step is attempted in order. An attempt is built with the step's factory, then run. An attempt that returns `Outcome.success()` ends with `step_succeeded`; one that throws ends with `step_abnormal_termination`, and a pure function, `afterAbnormalTermination`, decides what follows: a retry while `retry on abnormal termination` is on and the retry budget lasts, otherwise `step_given_up`, with the cause `abnormal termination` for a budget of 0 or with retries off, and `retries exhausted` once a budget greater than 0 is spent. A step given up fails the journey. Stage 5 adds the other outcomes and stage 6 the rest of the decisions.
- **The clock.** The engine stamps each event with `Instant.now(clock)`, the system's UTC clock in `LocalExecutor`, and a fixed one in the tests.
- **What is caught.** Every call into custom code goes through `calling`, which catches everything but an `Error`. An `Error` reaches whoever called `run`.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: the events of a journey whose steps succeed, numbering and stamping, an abnormal termination with and without retries, retries exhausted, a step that cannot be built, and an `Error` that is never caught.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DecisionTest.kt`: what follows an abnormal termination, for each budget and setting.
- `itinera/src/test/kotlin/org/itineradev/itinera/executor/LocalExecutorTest.kt`: a journey refused while one runs.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a Java workflow built with lambdas runs on `LocalExecutor`.

## Done when

12 is listed in both manifests once every scenario tagged `@proposal-0012` passes in both runners.
