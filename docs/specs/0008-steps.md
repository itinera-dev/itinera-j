# 0008: Steps

Tech spec for [proposal 0008](https://github.com/itinera-dev/spec/blob/main/proposals/0008-steps.md), implemented in [#4](https://github.com/itinera-dev/itinera-j/issues/4). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 4 and 7 above all, and [0062](0062-workflow-descriptors.md) the step descriptors and how steps declare what they request.

Status: done. Stage 3 built a step for each attempt and ran it, and stage 4 let it declare what it requests. Stage 5 resolves those requests before each attempt is built, lets a step fail, skip and contribute, then gives it its step reporter. Stage 7 calls the hooks that react to steps, and lists 8 in both manifests.

## Building a step for each attempt

Every attempt of a step is built anew, by its factory, in `engine/Attempts.kt`. Before the factory is called, `Building`, in `engine/Building.kt`, resolves every request the step declared, as chapter 4, 4.2 says, so a built step has all its inputs and services and a read never waits:

1. **Inputs first**, in the order the step declares them, by `Inputs`, in `engine/Inputs.kt`: through the step's input adapter, as [0060](0060-input-adapters-attached-to-steps.md) says, or from the data bag, by `fromWorkflow`, in `engine/WorkflowData.kt`. A required input with no value aborts the journey with `required data missing`, naming the step and the key; an optional one is absent, `null` in Kotlin and an empty `Optional` through `itinera-java`'s `JavaRequests.optionalInput`, and emits `optional_input_absent`, naming the step and the attempt. A value of another type aborts the journey with `wrong type`, naming the step and the key. Each input is the step's own copy, as [0121](0121-read-only-copies.md) says.
2. **Services next**, in the order the step declares them, by `Lending`, in `engine/Lending.kt`, as [0095](0095-services-provided-to-steps.md) says. A step whose inputs abort the journey never causes a provider to be called.
3. **Then the rest**: the journey ID, the step's name and the attempt are answered as asked, by `Call`, in `engine/Call.kt`.

The answers reach the factory as an `Answered`, in `engine/Answered.kt`, the engine's `Got`: reading a request the step did not declare throws `IllegalStateException`, which the engine catches as any failure of the factory, so the step could not be built. Until the step reporter is made, reading it throws in the same way, saying this version does not resolve it yet.

A step that cannot be built aborts the journey with `step could not be built`, and never enters the retry loop, as chapter 4, 4.2 point 7 says.

## Outcomes

A step's run returns an `Outcome`, in `org.itineradev.itinera.step`, a sealed interface whose members are the data object `Success` and the data classes `Failure`, holding a `Reason`, and `Skipped`, holding a `Reason` or `null`, as chapter 4, 4.3 says. Their constructors are internal, so every outcome is made by a factory that captures the reason's details: `Outcome.success()`, `Outcome.failure(reason)` or `Outcome.failure(code)`, and `Outcome.skipped()`, `Outcome.skipped(reason)` or `Outcome.skipped(code)`, static from Java. `ItineraStep` calls them from its default methods `succeed()`, `fail(reason)`, `fail(code)`, `skip()`, `skipBecause(reason)` and `skipBecause(code)`, the names of Rust's helpers. A failure carries no retry flag.

`Attempting`, in `engine/Attempting.kt`, builds and runs one attempt, then gives how it `Ended`, in `engine/Ended.kt`. `Attempts`, in `engine/Attempts.kt`, emits the outcome fact and what follows it:

1. **Success**: `step_succeeded`, then the attempt's contributions are committed, and the next step follows.
2. **Skipped**: `step_skipped`, with the reason when there is one, then `contributions_discarded`, and the next step follows. The step is never attempted again, and no hook is consulted, as chapter 4, 4.5 says.
3. **Failure**: `step_failed`, with the reason, then the decision of `afterFailure`, in `engine/Decision.kt`: a retry, with `step_retrying` and the cause `failure`, while the retry budget lasts; otherwise `step_given_up`, with the cause `failure` for a budget of 0 and `retries exhausted` once a budget greater than 0 is spent, and the journey fails with the last reason. `retry on abnormal termination` does not apply. Stage 6 plans the decisions, but a step that fails needs one to end its journey, so the executor's own rule lands here, as itinera-ts did; stage 7 lets hooks change it.
4. **Abnormal termination**: as before, `step_abnormal_termination` and the decision of `afterAbnormalTermination`. It carries no contributions.

Once the run returns, the engine captures the reason's details again, in `Attempting`, in `engine/Attempting.kt`, since the step can still reach the copy its outcome holds, and the events carry that second copy, which no application reaches. A reason whose details are not a value, which the factories keep as given, is found then, and aborts the journey with `not a value`, naming the step, with no outcome fact, as chapter 4, 4.3 point 3 says.

## Contributions

`Requests.contributor()` gives a step the `Contributor` of its attempt, `Contributions`, in `engine/Contributions.kt`, made before the attempt is built and closed when it ends, whether it returned, threw or aborted the journey, so a contributor kept beyond its attempt throws `IllegalStateException` where it is called and changes nothing, as chapter 4, 4.6 point 7 says. Each value is captured when it is contributed, the last under a key winning. The contributions are kept until the attempt ends, so a later stage can show them to the attempt's hooks whatever the outcome, and only a success commits them, through `Journey.commit`, in `engine/Journey.kt`: for each key, in the order first contributed, the value enters the data bag, `contribution_committed` is emitted, naming the step and the attempt as its source, and `data_overwritten` follows when the key held a value. A reporter failing on one of these events aborts the journey, and the commits stop.

A contribution that is not a value aborts the journey with `not a value`, naming the step and the key, and the call ends the step by throwing the executor's signal, `JourneyAbortSignal`, an internal class no application can name. The abort is recorded before the signal is thrown, and the engine looks for a recorded abort once the factory returns or throws, and once the run has returned or thrown and the attempt's handles are closed, so a step that catches the signal, as `catch (e: Exception)` would, still ends the journey aborted, with no outcome fact and no commit, as chapter 2, 2.9 point 5 says. A contribution made after the journey was aborted throws the signal again.

## The step reporter

`Requests.stepReporter()` gives a synchronous step its `StepReporter`, and `Requests.asyncStepReporter()`, in `itinera-kotlin`, gives an asynchronous step its `AsyncStepReporter`. The mode's `ReporterHandles`, which `Parts.reporterHandles` gives, makes it for each attempt: `EmittingReporter`, in `engine/EmittingReporter.kt`, or `EmittingAsyncReporter`, in `itinera-kotlin`'s `executor/async`. Each is a `ReporterHandle`, in `engine/Emits.kt`, that emits through the attempt's `StepEmits`, in `engine/StepEmits.kt`, and lets one call through at a time, whichever thread or coroutine makes it, the synchronous one under a lock and the asynchronous one under a `Mutex`, so a step that hands its reporter to threads of its own still gets events with distinct sequences in the order their calls went through. `Attempting` makes it with the contributor and closes it with it when the attempt ends, once no call is going through. A call on a closed reporter throws `IllegalStateException` and reports nothing, as chapter 4, 4.6 point 7 says and [0057](0057-handles-valid-during-their-attempt.md) describes.

- **Emits.** `info`, `warning` and `error`, each with a message and optionally data, emit `step_info`, `step_warning` and `step_error`, naming the step and the attempt, with a copy of the data captured when it is emitted, or `null` when none was given. A call returns once every reporter has the event, so events keep the order of the calls. The synchronous reporter runs the engine's `suspend` emit to its end with `driven`, since the synchronous mode never suspends; the asynchronous reporter awaits it. Data that is not a value aborts the journey with `not a value` in the event, and the event is not delivered ([0064](0064-event-data-values.md)).
- **Global metadata.** `setGlobalMetadata` sets an entry through `Journey.setGlobalMetadata`, which [0102](0102-global-metadata.md) describes.
- **A reporter that fails.** When a reporter or the dispatcher fails on an event or an entry a step gave, the journey is aborted with `reporter failed` and the call ends the step, as [0055](0055-reporter-fails-while-custom-code-runs.md) describes.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: "a step reads its inputs from the data bag", "an optional input with no value is absent, and optional_input_absent names the step", "a required input with no value aborts the journey before the step is built" and "an input of another type aborts the journey with wrong type, naming the step".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuildingTest.kt`: "a step's inputs are resolved before its services".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: "a step whose factory reads a request it did not declare cannot be built".
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous input adapter and service provider are awaited while a step is built".
- `itinera/src/test/kotlin/org/itineradev/itinera/step/OutcomeTest.kt`: each helper of `ItineraStep` makes the outcome its factory makes, one named case each, and the details of a failure and a skip are captured when made.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptsTest.kt`: a failure given up, retried, and past its budget; a skip that discards its contributions; a success that commits its contributions, and records an overwrite; only the successful attempt's contributions committed; an abnormal termination with no contributions.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DecisionTest.kt`: "a failure is retried while the budget lasts, and otherwise gives the step up", one named case for each budget.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: a step that catches the executor's signal, or lets it escape, ends the journey aborted; a reason whose details are not a value; a contributor used after its attempt.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ContributionsTest.kt`: the last value wins, a contribution is captured when made, and one that is not a value aborts the journey.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepEmitsTest.kt`: "each emit delivers its own event, naming the step, the attempt and the message", one named case each; an event carries a copy of its data; data that is not a value; a reporter that fails on an event ends the step at the emit, even when the step catches it; a step reporter used after its attempt throws and reports nothing.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EmittingReporterTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/EmittingAsyncReporterTest.kt`: calls from several threads or coroutines go through one at a time, and closing waits for the call going through.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous step's reporter emits and sets global metadata, awaiting every reporter".
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: `aJavaStepFailsWithAReasonAndItsContributionsAreNeverCommitted`, `aJavaStepThatSkipsLetsTheJourneyGoOn` and `aJavaStepEmitsAndSetsGlobalMetadataThroughItsStepReporter`.

## Done when

8 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #4`: every scenario tagged `@proposal-0008` passes in both runners and every column, but those that also name a proposal not listed yet.
