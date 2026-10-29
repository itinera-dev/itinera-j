# 0009: Running a workflow

Tech spec for [proposal 0009](https://github.com/itinera-dev/spec/blob/main/proposals/0009-running-a-workflow.md), implemented in [#5](https://github.com/itinera-dev/itinera-j/issues/5). [0012](0012-local-executor.md) describes the executors, [0024](0024-abnormal-termination-retriable.md) and [0106](0106-retry-budget-alone.md) the decision after a failed attempt, and [0010](0010-hooks-lifecycles-and-roles.md) the hooks around it.

Status: done. Stage 3 ran the steps in order, stage 5 added outcomes, contributions and the decision after a failure, stage 6 proved that no step runs once a step is given up or the journey aborted, and stage 7 calls the hooks the scan names, and lists 9 in both manifests.

9 is listed in both manifests. Its API is the executors' `run` and the result, which carry no `@ExperimentalItineraApi`.

## The scan

`Attempts`, in `engine/Attempts.kt`, runs each step of the descriptor in the order declared, one attempt at a time, through `Attempting`, in `engine/Attempting.kt`:

1. **An attempt starts.** `attempt_started`, then the step's policies are built for the attempt ([0058](0058-building-policies.md)), then the step is built from what it requests and run.
2. **Success.** `step_succeeded`, the attempt's contributions committed, then `on step success`. Unless that hook ends the journey, the scan moves to the next step.
3. **Skipped.** `step_skipped`, its contributions discarded, and the scan moves on. No hook is called.
4. **Failure or abnormal termination.** `on step abnormal termination` first, for an abnormal termination. Then `decide`, in `engine/Decision.kt`, retries the step while its retry budget allows, after `on step retry` and `step_retrying`, or gives it up with `step_given_up`, then `on step failure`, and the journey fails at that step.
5. **The end.** Once every step is done the journey succeeds. `on workflow success` or `on workflow failure` runs before the journey's last event, and the result carries the data bag.

A lifecycle a step hook returns ends the scan at once ([0010](0010-hooks-lifecycles-and-roles.md)). An abort, from a step that could not be built, a hook that failed or a reporter, ends it with `journey_aborted` last, and no workflow hook runs ([0085](0085-aborted-result-has-no-data.md)). `Early`, in `engine/Early.kt`, carries how a journey ends before its last step is done.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: the steps in order, no step after one is given up or after an abort, and the workflow hooks before the last event.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptsTest.kt`: what each outcome commits or discards, and retries within the budget.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepHooksTest.kt`: where each step hook is called in the scan.

## Done when

9 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #5`: every scenario tagged `@proposal-0009` passes in both runners and every column, but those that also name a proposal not listed yet.
