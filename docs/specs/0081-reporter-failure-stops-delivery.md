# 0081: A reporter that fails stops the delivery of the event it failed on

Tech spec for [proposal 0081](https://github.com/itinera-dev/spec/blob/main/proposals/0081-reporter-failure-stops-delivery.md), implemented in [#27](https://github.com/itinera-dev/itinera-j/issues/27). [0011](0011-events.md) describes reporters and dispatchers.

Status: done. Stage 1 made the default dispatchers stop at a reporter that throws. Stage 3 added the executor's guard around each reporter, which records the abort and keeps the rules whatever the dispatcher. Stage 7 runs the scenarios whose reporters fail on a hook's events, and lists 81 in both manifests.

## How it is built

1. **The default dispatchers** call their reporters in the order they were added. When one throws on an event other than `journey_aborted`, they stop and throw what it threw: the reporters after it do not receive the event, as point 2 requires.
2. **`journey_aborted`** reaches every reporter the default dispatchers hold, whatever they throw. The dispatcher then throws the first failure, which the executor ignores, as point 6 says.
3. **An `Error`** thrown by a reporter is never caught, by the dispatchers or by the executor.
4. **Whatever the dispatcher.** The executor wraps each reporter it adds in a `Guard`, in the engine, which all the journey's guards share a record of failures with. When the guarded reporter throws, the guard records its error and rethrows it, and the engine aborts the journey with `reporter failed`, naming the step being run, if any; it does the same when a dispatcher swallows a guard's failure. From then on it passes nothing to that reporter, and only `journey_aborted` to the others, so points 3 and 4 hold even for a dispatcher that goes on delivering.
5. **Cancellation.** The asynchronous dispatcher catches whatever a reporter throws on `journey_aborted`, a `CancellationException` included, since a reporter's own timeout throws one that point 6 must ignore. So the asynchronous executor checks whether its own coroutine was cancelled once `journey_aborted` is delivered, and abandons the journey if it was, as section 11 of the plan says; `AsyncLocalExecutorTest` shows it.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: "a reporter that throws stops the delivery of the event, and the dispatcher throws it", "journey_aborted reaches every reporter, and the dispatcher throws the first failure", and "an Error from a reporter is never caught, even on journey_aborted".

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/GuardTest.kt`: a reporter that fails aborts the journey naming the step, the reporters after it get only `journey_aborted` and it gets nothing more, the rules hold for a dispatcher that goes on delivering, a failure on `journey_aborted` is ignored, and an `Error` is never caught.

## Done when

81 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #27`: every scenario tagged `@proposal-0081` passes in both runners and every column, but those that also name a proposal not listed yet.
