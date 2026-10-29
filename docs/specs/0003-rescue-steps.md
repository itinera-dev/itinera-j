# 0003: Rescue steps

Tech spec for [proposal 0003](https://github.com/itinera-dev/spec/blob/main/proposals/0003-rescue-steps.md), implemented in [#3](https://github.com/itinera-dev/itinera-j/issues/3). Its rules live in [chapter 7](https://github.com/itinera-dev/spec/blob/main/spec/07-rescue-steps.md) of the specification. The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 5 to 7 above all, [0062](0062-workflow-descriptors.md) how a rescue step is declared and checked, [0098](0098-rescue-step-adapters-and-providers.md) the input adapters and service providers attached to a rescue step, and [0122](0122-unexpected-results.md) what a rescue step that returns `null` does.

Status: started. Stage 4 declared rescue steps, with the builders and with annotations, refused `rescued step retried` and a step named `<step name>_rescue`, and listed each step's rescue step. Stage 9 runs them; its last pull request lists 3 in both manifests.

## Declaring a rescue step

1. **With the builder**, `RescueStepDescriptor.builder(factory)` in the synchronous mode, and `asyncRescueStep(factory)` in `itinera-kotlin`, take a `SyncRescueFactory` or an `AsyncRescueFactory`, which builds the rescue step from what it requested and returns its run: a `SyncRescueRun` or an `AsyncRescueRun`, whose `run` returns a `RescueOutcome`. A step's builder takes the descriptor with `rescue(...)`, once.
2. **With annotations**, a class marked `@RescueStep` and implementing `ItineraRescueStep` is named by `@WithStep(rescue = ...)`. Its constructor takes `@Input` parameters, `@FromRescuedStep` for what the failed attempt contributed, and `Reason` and `Throwable` by their types, each optional when its type admits `null`, or, in Java with `itinera-java`, is an `Optional`. Its `run` takes services, marked `@Service`, its contributor and its step reporter, and returns `RescueOutcome`; in an asynchronous workflow it is `suspend`.
3. **`RescueOutcome`** is a sealed interface of two members: `Success`, made by `RescueOutcome.success()` or `ItineraRescueStep.succeed()`, and `Failure`, which carries a `Reason`, made by `RescueOutcome.failure(reason)` or `failure(code)`, or `ItineraRescueStep.fail(reason)` or `fail(code)`. A reason's details are copied when the outcome is made, and again once the run returns it, as for a step's `Outcome`.

## Running a rescue step

`Attempts`, in `engine/Attempts.kt`, hands a failed attempt to `Rescuing`, in `engine/Rescuing.kt`, as chapter 7 says:

1. **When.** The step has a rescue step, and its attempt 1 reported a failure, or ended in an abnormal termination after which `on step abnormal termination` did not return `FailWorkflow`, whatever `retry on abnormal termination` says. Since a rescued step's retry budget is 0, the attempt that follows the rescue is attempt 2, so a step is rescued at most once. `on step retry` and `on step failure` are not called for the attempt.
2. **`step_rescuing`** names the step, the attempt, the cause, `failure` or `abnormal termination`, and the rescue step. The journey then names the rescue step, `<step name>_rescue`, so an abort while it runs names it.
3. **The rescue step's attempt.** `attempt_started` names it with attempt 1, then `Attempting`, in `engine/Attempting.kt`, builds and runs it as it does a step's attempt, with a contributor and a step reporter of its own, closed once it returns. `Building`, in `engine/Building.kt`, resolves what it requests, in the order declared: its inputs as for any step, and what it requests from the step it rescues, from what the failed attempt left, as for a step hook ([0010](0010-hooks-lifecycles-and-roles.md)): the contributions by key, the reason and its own copy of the error ([0027](0027-received-data-read-only.md)), a required one that is missing aborting the journey with `required data missing`, naming the rescue step and the key, or the failure reason or the error, which `ByRescueStep` names, and an optional contribution that is missing emitting `optional_input_absent`, naming the rescue step. Its services come next. The mode's `StepCaller` builds it: `SyncParts` calls the `SyncRescueFactory`, and `AsyncParts`, in `itinera-kotlin`, awaits the `AsyncRescueRun`.
4. **Success.** `step_succeeded` and the rescue step's `contribution_committed` events follow, then `step_rescued` names the step again, and the step is attempted again, with new step policies.
5. **Failure or abnormal termination.** `step_failed` or `step_abnormal_termination`, then `step_given_up` for the rescue step, with the cause `failure` or `abnormal termination`, decided by `default`, with no hook called. The journey fails, and `journey_failed` names the rescue step, with its reason or its error. The rescued step never runs again.
6. **No skip.** `RescueOutcome` has no skip, so the engine never sees one, and `rescue skipped` is never recorded; the manifests list `rescue-skip` as impossible.
7. **Aborts.** A factory that throws aborts the journey with `step could not be built`, naming the rescue step; a factory or a run that returns `null` with `unexpected result`, as [0122](0122-unexpected-results.md) says.

The engine keeps no step statuses: the runners derive `Rescuing` and every other status from the events.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptsTest.kt`: "an abnormal termination is rescued, though the step retries abnormal terminations", "on step abnormal termination is called before the step is rescued, and on step retry never", "FailWorkflow from on step abnormal termination gives the step up instead of rescuing it" and "a step that fails again after its rescue is given up, calls on step failure, and is not rescued again".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/RescuingTest.kt`: "a step that fails is rescued, then attempted again", "the rescue step's contributions are committed before step_rescued", "a rescue step that fails is given up, and fails the journey with its reason", "a rescue step that ends in an abnormal termination fails the journey with its error" and "a reporter failing while a step is rescued names the step the journey is on", on `step_rescuing`, on the rescue step's success and on `step_rescued`.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: "a rescue step whose failure's details are not a value aborts the journey, naming it", "a rescue step that cannot be built aborts the journey, naming it" and "a rescue step that gives null is an unexpected result naming it".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuildingTest.kt`: "a rescue step receives what the failed attempt contributed, and its reason", "a rescue step receives its own copy of the error that ended the attempt", "a rescue step's required reason that the attempt did not leave aborts the journey, naming it", "optional data the failed attempt did not contribute is absent, naming the rescue step" and "a rescue step's inputs come through the adapter attached to its own name, never its step's" and "a rescue step's service comes from the provider attached to its own name, never its step's".
- `itinera/src/test/kotlin/org/itineradev/itinera/step/RescueOutcomeTest.kt`: "a rescue step's helper makes the outcome the static factory makes" and "a rescue step's failure's details are captured when it is made".
- `itinera/src/test/kotlin/org/itineradev/itinera/workflow/RescueStepDescriptorTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/kotlin/KotlinRescueClassesTest.kt`: a rescue step built from its answers succeeds or fails.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous rescue step is awaited, then its step attempted again".
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a Java rescue step that succeeds or fails, and one whose factory or run returns `null`.
- `itinera/src/test/java/org/itineradev/itinera/workflow/JavaRescueClassesTest.java`: "aJavaRescueStepFailsThroughItsInterfacesHelpers".

## Done when

3 is listed in both manifests once every scenario tagged `@proposal-0003` passes in both runners and every column, but `A rescue step that skips aborts the journey`, which both manifests exclude as `rescue-skip`.
