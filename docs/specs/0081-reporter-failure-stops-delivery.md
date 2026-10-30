# 0081: A reporter that fails stops the delivery of the event it failed on

Tech spec for [proposal 0081](https://github.com/itinera-dev/spec/blob/main/proposals/0081-reporter-failure-stops-delivery.md), implemented in [#27](https://github.com/itinera-dev/itinera-j/issues/27). [0011](0011-events.md) describes reporters and dispatchers.

Status: started. Stage 1 made the default dispatchers stop at a reporter that throws. Stage 3 adds the executor's guard around each reporter, which records the abort and keeps the rules whatever the dispatcher.

## How it is built

1. **The default dispatchers** call their reporters in the order they were added. When one throws on an event other than `journey_aborted`, they stop and throw what it threw: the reporters after it do not receive the event, as point 2 requires.
2. **`journey_aborted`** reaches every reporter the default dispatchers hold, whatever they throw. The dispatcher then throws the first failure, which the executor ignores, as point 6 says.
3. **An `Error`** thrown by a reporter is never caught, by the dispatchers or by the executor.
4. **Whatever the dispatcher.** Stage 3 wraps each reporter the executor adds in a guard. When the guarded reporter throws, the guard records the abort with `reporter failed`. From then on it passes nothing to that reporter, and only `journey_aborted` to the others, so points 3 and 4 hold even for a dispatcher that goes on delivering.
5. **Cancellation.** The asynchronous dispatcher catches whatever a reporter throws on `journey_aborted`, a `CancellationException` included, since a reporter's own timeout throws one that point 6 must ignore. So stage 3's asynchronous executor checks whether its own coroutine was cancelled once `journey_aborted` is delivered, and abandons the journey if it was, as section 11 of the plan says.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: "a reporter that throws stops the delivery of the event, and the dispatcher throws it", "journey_aborted reaches every reporter, and the dispatcher throws the first failure", and "an Error from a reporter is never caught, even on journey_aborted".

Stage 3 adds the tests of the guard.

## Done when

81 is listed in both manifests once every scenario tagged `@proposal-0081` passes in both runners.
