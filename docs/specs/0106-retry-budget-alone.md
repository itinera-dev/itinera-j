# 0106: The retry budget alone decides retries

Tech spec for [proposal 0106](https://github.com/itinera-dev/spec/blob/main/proposals/0106-retry-budget-alone.md), implemented in [#35](https://github.com/itinera-dev/itinera-j/issues/35). [0008](0008-steps.md) describes outcomes, and [0024](0024-abnormal-termination-retriable.md) the setting for abnormal terminations.

Status: done. Stage 3 gave a failure no retriable flag. Stage 6 decides retries by the budget alone. Stage 7 calls `on step retry` and `on step failure` with their causes, lets `on step retry` stop the retries, and lists 106 in both manifests. Rescue steps, whose rules 0106 also amends, wait for 0003, in stage 9.

## API

106 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`.

- **`Outcome.failure(reason)`**, in `org.itineradev.itinera.step`: a failure carries its reason and nothing else; `StepFailed` carries the step, the attempt and the reason.
- **`StepBuilder.retryBudget(retries)`** and **`@WithStep(retryBudget = ...)`**: the budget, 0 unless given, and never below 0.
- **`StepBuilder.noRetryOnAbnormalTermination()`** and **`@WithStep(noRetryOnAbnormalTermination = true)`**: turn off the retries of abnormal terminations, on by default.
- **`FailureCause`**, in `org.itineradev.itinera.journey`: `FAILURE`, `ABNORMAL_TERMINATION`, `RETRIES_EXHAUSTED` and `FAIL_WORKFLOW`.

## How it is built

1. `decide`, in `engine/Decision.kt`, retries a failure, and an abnormal termination while the setting is on, while the budget allows another attempt; once a budget above 0 is spent the step is given up with `retries exhausted`, and with a budget of 0 at once with the cause of its only attempt.
2. `Attempts`, in `engine/Attempts.kt`, calls `on step retry` with the cause `failure` or `abnormal termination` before `step_retrying`, and never when no retry is possible. A hook that returns `FailWorkflow` gives the step up with the cause `FailWorkflow`, and `on step failure` is not called.
3. `retries exhausted` carries the reason of the last attempt, if it reported a failure, and its error, if it ended in an abnormal termination ([0083](0083-what-failures-carry.md)).

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DecisionTest.kt`: retries by the budget, for failures and abnormal terminations.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptsTest.kt`: a failure with no budget, within the budget and past it, with the last reason.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepHooksTest.kt`: the causes the hooks receive, and `on step retry` returning `FailWorkflow`.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: "a hand-written step with a retry budget below 0 is refused".

## Conformance

Every scenario tagged `@proposal-0106` passes in both runners and every column, but those that also name 0003, which wait for stage 9.

## Done when

106 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #35`.
