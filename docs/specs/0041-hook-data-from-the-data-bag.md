# 0041: Hooks read data from the workflow without input adapters

Tech spec for [proposal 0041](https://github.com/itinera-dev/spec/blob/main/proposals/0041-hook-data-from-the-data-bag.md), implemented in [#13](https://github.com/itinera-dev/itinera-j/issues/13). [0010](0010-hooks-lifecycles-and-roles.md) describes hooks and their requests.

Status: done. Stage 4 let hooks request data from the workflow. Stage 7 resolves it from the data bag before each hook runs, and lists 41 in both manifests.

## API

41 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`.

- **`Requests.fromWorkflow(key, token)`** and its optional form are the same requests a step makes, taken by every hook's kind; in `itinera-java`, `JavaRequests.optionalFromWorkflow` answers with an `Optional`.
- **`@FromWorkflow`** marks such a parameter of an annotated hook.

## How it is built

1. A hook's data from the workflow is read from the data bag under its key, by type, the hook's own copy. Input adapters serve steps only: `Hooks`, in `engine/Hooks.kt`, answers a hook's data from the workflow through the journey's `fromWorkflow`, never through the adapters attached to the step it acts on.
2. Every request is resolved, in the order the hook declares it, before the hook is called. A required key that holds nothing aborts the journey with `required data missing`, and a value of another type with `wrong type`, required or optional; `journey_aborted` names the policy, the hook and the key, and the hook has no `hook_called`.
3. `optional_input_absent` names the policy and the hook as well as the key, and comes before the hook's own events and its `hook_called`.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/HooksTest.kt`: "a required request for data the workflow lacks aborts the journey before the hook runs", "an optional request for data the step did not contribute is absent, naming the hook" and "a workflow hook's requests name it".
- `itinera-java/src/test/java/org/itineradev/itinera/request/java/JavaOptionalHookRequestsTest.java`: optional requests from Java.

## Conformance

Every scenario tagged `@proposal-0041` passes in both runners and every column, among them the one whose step has an input adapter for the key the hook reads.

## Done when

41 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #13`.
