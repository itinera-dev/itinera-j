# 0055: A reporter that fails while custom code runs

Tech spec for [proposal 0055](https://github.com/itinera-dev/spec/blob/main/proposals/0055-reporter-fails-while-custom-code-runs.md), implemented in [#17](https://github.com/itinera-dev/itinera-j/issues/17). [0081](0081-reporter-failure-stops-delivery.md) describes the guard that records a reporter's failure, and [0008](0008-steps.md) the step reporter.

Status: done. Stage 5 delivers what a step emits before its call returns, and ends the step at the call a reporter failed on. Stage 7 does the same for hooks, through `HookEmits` in `engine/HookEmits.kt`, and lists 55 in both manifests. Stage 9 does it for rescue steps, whose scenario runs once 0003 is listed.

## How it is built

1. **Blocking delivery.** Every call on a step reporter goes through the attempt's `StepEmits`, in `engine/StepEmits.kt`, to `Journey.emit` or `Journey.setGlobalMetadata`, in `engine/Journey.kt`, which return once the dispatcher has given the event or the entry to every reporter. The synchronous reporter, `EmittingReporter`, runs that to its end with `driven` before it returns, since the synchronous mode never suspends; the asynchronous reporter, `EmittingAsyncReporter`, in `itinera-kotlin`, awaits it.
2. **One call at a time.** A step may hand its reporter to threads or coroutines of its own. Each reporter lets one call through at a time, the synchronous one under a lock and the asynchronous one under a `Mutex`, so the events of an attempt carry distinct sequences that follow the order their calls went through, and no event is lost.
3. **A reporter that fails.** When a reporter or the dispatcher fails on an event or an entry, the guard records the failure, `Journey` aborts the journey with `reporter failed`, naming the step, and throws the executor's signal, `JourneyAbortSignal`, an internal class no application can name. The call ends the step there, as point 2 asks.
4. **The abort stands.** The abort is recorded before the signal is thrown. Every later call on the step reporter, or on the contributor, throws the signal again, so nothing more the step emits is delivered, and once the step returns or throws and its handles are closed, `Attempting`, in `engine/Attempting.kt`, finds the recorded abort and ignores what the step returned. Looking only after closing matters: closing the step reporter waits for a call going through, which a thread the step started may make as the attempt ends, so a reporter that fails on it still aborts the journey. Either way there is no outcome fact, no commit, no retry and no `step_abnormal_termination`, as points 3 and 4 say. `journey_aborted` is then the last event.

## Dependencies

[0081](0081-reporter-failure-stops-delivery.md), for the guard that records a reporter's failure whatever the dispatcher does.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepEmitsTest.kt`: "a reporter that fails on an event ends the step at the emit, even when the step catches it" and "a reporter that fails on an event a step's thread emits as the attempt ends still aborts the journey".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/JourneyTest.kt`: "a reporter that fails on an entry aborts the journey with reporter failed".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: "a step that catches the executor's signal still ends the journey aborted".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EmittingReporterTest.kt`: "calls from several threads go through one at a time, so their events keep distinct increasing sequences".
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/EmittingAsyncReporterTest.kt`: "calls from several coroutines go through one at a time".
- The conformance runners pass every scenario tagged `@proposal-0055`, the hook's included, but the rescue step's, which waits for 0003 in stage 9.

## Done when

55 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #17`: every scenario tagged `@proposal-0055` passes in both runners and every column, but those that also name a proposal not listed yet.
