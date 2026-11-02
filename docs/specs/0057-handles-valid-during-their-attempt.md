# 0057: Handles valid only during their attempt

Tech spec for [proposal 0057](https://github.com/itinera-dev/spec/blob/main/proposals/0057-handles-valid-during-their-attempt.md), implemented in [#19](https://github.com/itinera-dev/itinera-j/issues/19). [0008](0008-steps.md) describes the contributor and the step reporter.

Status: started. Stage 5 closes a step's contributor and step reporter when its attempt ends. Stage 7 closes a hook's contributor and means of emitting when the hook returns, and its scenario, whose hook uses the handles a step kept, passes then.

## How it is built

1. **Made for the attempt.** `Attempting`, in `engine/Attempting.kt`, makes the attempt's contributor, `Contributions`, in `engine/Contributions.kt`, and its step reporter, a `ReporterHandle` that the mode's `ReporterHandles` makes over the attempt's `StepEmits`, before the step is built, and gives them to the step only through the requests it declared.
2. **Closed when it ends.** Once the step returns, throws, or the journey is aborted, `Attempting` closes both in a `finally`. Closing the step reporter waits for a call going through, under the same lock or `Mutex` that lets one call through at a time, so no event of the attempt is delivered after it ends. Closing twice changes nothing. `Attempting` looks for a recorded abort only once both are closed, so an abort that such a call recorded before the step reporter closed still ends the journey.
3. **A late call fails where it is made.** A call on a closed contributor or step reporter throws `IllegalStateException`, saying the attempt has ended, before anything is captured or delivered: nothing is contributed, no event is emitted and no reporter is called, as points 2 and 3 say. The throw reaches only the code that made the call and changes nothing in the journey.
4. **Not impossible to express.** The JVM has no lifetimes, so a step can always keep its handles, and the scenario tagged `@late-handle` runs, as the plan's table of tags says.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: "a contributor used after its attempt throws and changes nothing".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepEmitsTest.kt`: "a step reporter used after its attempt throws and reports nothing".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EmittingStepReporterTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/EmittingAsyncStepReporterTest.kt`: "closing waits for the call going through".

## Done when

57 is listed in both manifests once every scenario tagged `@proposal-0057` passes in both runners.
