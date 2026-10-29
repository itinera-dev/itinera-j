# 0054: How custom code fails

Tech spec for [proposal 0054](https://github.com/itinera-dev/spec/blob/main/proposals/0054-how-custom-code-fails.md), implemented in [#16](https://github.com/itinera-dev/itinera-j/issues/16). [0049](0049-custom-code-that-throws.md) lists every call into custom code, and the [tier 1 plan](../tier-1-plan.md), section "Rules made impossible, and rules enforced at run time", which rules each language makes impossible.

Status: done. Stage 3 left an `Error` uncaught and named the abort reasons. Stage 6 listed `mode-not-accepted` as impossible in both manifests, with its proofs. Stage 7 does the same for `invalid-lifecycle`, and lists 54 in both manifests.

## API

54 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`: `AbortReason.HOOK_FAILED` and `AbortReason.REPORTER_FAILED`, in `org.itineradev.itinera.journey`, read `hook failed` and `reporter failed`.

## How it is built

1. **A recoverable failure** is any `Throwable` that is not an `Error`, thrown by custom code; its consequence depends on what threw, as 0049's table says.
2. **An unrecoverable failure** is an `Error`. `rethrowError`, in `engine/Errors.kt`, throws it again from every catch, so the journey stops with no further event, `run` produces no result, and the `Error` reaches whoever called `run`.
3. **Rules made impossible.** Each is proven by a compile-fail test in Kotlin and in Java, and refused when an annotated workflow is described, before any instance:
   - `invalid-lifecycle`: a failure hook returns `OnFailure`, which has no `finishWorkflow`, so it cannot finish the workflow ([0010](0010-hooks-lifecycles-and-roles.md));
   - `mode-not-accepted`: `LocalExecutor.run` takes only a `WorkflowInstance<Sync>`, and `AsyncLocalExecutor.run` only a `WorkflowInstance<Async>` ([0012](0012-local-executor.md)).

   The other tags, `role-not-provided`, `non-value` and `late-handle`, are checked at run time, so their scenarios run.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`, `GuardTest.kt` and `OwnErrorsTest.kt`: an `Error` is never caught.
- `itinera/src/test/kotlin/org/itineradev/itinera/journey/AbortReasonTest.kt`: the reasons' names.
- The proofs listed under `impossible` in both manifests: `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/policy/PolicyTypesTest.kt` and `itinera-java/src/compileFailTest/java/org/itineradev/itinera/policy/JavaPolicyTypesTest.java` for `invalid-lifecycle`, and the proofs of `mode-not-accepted` that [0012](0012-local-executor.md) lists.

## Conformance

Every scenario tagged `@proposal-0054` passes in both runners and every column, but those tagged with a rule listed as impossible, which `run-conformance` excludes and the report lists as not applicable, with their proof.

## Done when

54 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #16`.
