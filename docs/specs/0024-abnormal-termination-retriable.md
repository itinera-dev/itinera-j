# 0024: Abnormal terminations are retried only when the step allows it

Tech spec for [proposal 0024](https://github.com/itinera-dev/spec/blob/main/proposals/0024-abnormal-termination-retriable.md), implemented in [#9](https://github.com/itinera-dev/itinera-j/issues/9). [0106](0106-retry-budget-alone.md) amends it: the setting is `retry on abnormal termination`, on unless the step turns it off, and the retry budget alone decides retries.

Status: done. Stage 3 made a step that throws end its attempt as an abnormal termination. Stage 6 decides what follows it, by the step's setting and its retry budget. Stage 7 calls `on step abnormal termination` and the failure hook with the cause they receive, and lists 24 in both manifests.

## API

24 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`.

- **`StepBuilder.noRetryOnAbnormalTermination()`** turns the setting off for a step built by hand, and **`@WithStep(noRetryOnAbnormalTermination = true)`** for a step of an annotated workflow. The setting is on unless one of them turns it off, with no workflow-wide default, as 0106 says.
- **`StepDescriptor.retryOnAbnormalTermination`** reads it.

## How it is built

1. A step's run that throws anything but an `Error` ends its attempt as an abnormal termination, carrying what it threw ([0049](0049-custom-code-that-throws.md)). It never aborts the journey.
2. `decide`, in `engine/Decision.kt`, gives the step up at once with the cause `abnormal termination` when the setting is off or the retry budget is 0, and otherwise retries it within the same budget as a failure, giving it up with `retries exhausted` once the budget is spent.
3. `Attempts`, in `engine/Attempts.kt`, calls `on step abnormal termination` before that decision is carried out ([0010](0010-hooks-lifecycles-and-roles.md)).

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/DecisionTest.kt`: "an abnormal termination is retried while it may be, and otherwise gives the step up".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepHooksTest.kt`: the hooks called after an abnormal termination, and the causes they receive.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt` and `itinera/src/test/java/org/itineradev/itinera/workflow/JavaAnnotatedWorkflowsTest.java`: the setting, by hand and through `@WithStep`.

## Conformance

Every scenario tagged `@proposal-0024` passes in both runners and every column.

## Done when

24 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #9`.
