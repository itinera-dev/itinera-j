# 0010: Hooks, lifecycles and workflow roles

Tech spec for [proposal 0010](https://github.com/itinera-dev/spec/blob/main/proposals/0010-hooks-lifecycles-and-roles.md), implemented in [#6](https://github.com/itinera-dev/itinera-j/issues/6). Chapter 6 of the specification defines the hooks, the [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, decision 17 how a role is recognised, [0058](0058-building-policies.md) when policies are built and [0049](0049-custom-code-that-throws.md) what a hook that throws does.

Status: done. Stage 4 declared policies, hooks and roles, in the builders and in annotated classes, and refused what is misdeclared. Stage 7 builds the policies, calls the hooks as a journey runs, lends the roles, runs the cases in both runners, and lists 10 in both manifests.

10 is listed in both manifests, so its public items carry no `@ExperimentalItineraApi`, but for `setGlobalMetadata` on `HookReporter` and `AsyncHookReporter`, which belongs to [0102](0102-global-metadata.md), not listed yet.

## API

- **Policies.** `StepPolicyDescriptor.builder(name, factory)` and `WorkflowPolicyDescriptor.builder(name, factory)` start a synchronous policy, and `asyncStepPolicy` and `asyncWorkflowPolicy`, in `itinera-kotlin`, an asynchronous one. Each hook is given with its requests, in the order they are answered, and a `SyncHook`, `SyncWorkflowHook`, `AsyncHook` or `AsyncWorkflowHook`, called with the policy and a `Got` of its answers. A class implementing `ItineraStepPolicy` or `ItineraWorkflowPolicy`, marked `@StepPolicy` or `@WorkflowPolicy`, declares one method per hook, marked `@OnStepSuccess`, `@OnStepFailure`, `@OnStepRetry`, `@OnStepAbnormalTermination`, `@OnWorkflowSuccess` or `@OnWorkflowFailure`.
- **Lifecycles.** `on step success` returns an `OnSuccess`: `keepDefault()`, `finishWorkflow()` or `failWorkflow(reason)`. The other step hooks return an `OnFailure`, which has no `finishWorkflow`, and the workflow hooks return nothing. `Lifecycle` holds `FinishWorkflow` and `FailWorkflow`, and `KeepDefault` is both an `OnSuccess` and an `OnFailure`. From Java, `keepsDefault`, `finishesWorkflow` and `failsWorkflow` read what a hook returned.
- **Requests.** A hook requests data from the workflow (`fromWorkflow`, [0041](0041-hook-data-from-the-data-bag.md)), data the step contributed (`fromStep`), the attempt's reason and error, the step's name, the attempt, the journey ID, the failure and retry causes, a contributor, a hook reporter and its roles. Each kind of hook takes only the requests its kind allows, through its `HookKind`, so a request a hook may not make does not compile. Annotated hooks request the same through their parameters, `@FromStep` and `@FromWorkflow` among them.
- **Roles.** An interface marked `@Role` is a role. A workflow provides one with `role(type)` on its builder, or by implementing it in an annotated class, and a hook requests it with `Requests.role(type)`, or the reified `role<R>()` in `itinera-kotlin`.

## How it runs

1. **Order.** `Attempts`, in `engine/Attempts.kt`, calls `on step success` once the attempt's contributions are committed, `on step abnormal termination` first after an abnormal termination, `on step retry` before `step_retrying`, and `on step failure` after `step_given_up`. `Engine`, in `engine/Engine.kt`, calls `on workflow success` or `on workflow failure` before the journey's last event, and none once the journey is aborted. `Hooks`, in `engine/Hooks.kt`, calls each policy's hook in the order the policies were attached.
2. **Requests.** A hook's requests are resolved in the order it declares them: data from the workflow, what the attempt left, from `ActingOn` in `engine/ActingOn.kt`, each value the hook's own copy, the facts of the call, and its roles.
3. **One call.** `Hooked`, in `engine/Hooked.kt`, gives each call a contributor and a hook reporter of its own, which close once the hook returns ([0057](0057-handles-valid-during-their-attempt.md)). `hook_called` follows the call, with the lifecycle returned, then the hook's contributions are committed, naming the policy and hook. Its events, `journey_info`, `journey_warning` and `journey_error`, go through `JourneyEmits`, in `engine/JourneyEmits.kt`, which a service provider's reporter shares ([0095](0095-services-provided-to-steps.md)).
4. **Lifecycles.** `lifecycleOf`, in `engine/Lifecycles.kt`, checks what a step hook returned. `keepDefault` lets the scan go on. `finishWorkflow` ends the journey at once as succeeded, decided by the hook. `failWorkflow` fails the journey with its reason, decided by the hook, and from `on step retry` or `on step abnormal termination` gives the step up for it, with no failure hook.
5. **Invalid lifecycles.** The types make it impossible for a failure hook to finish the workflow, in Kotlin and in Java, and an annotated failure hook declared to return `OnSuccess` is misdeclared when the workflow is described. `invalid-lifecycle` is therefore listed as impossible in both manifests. A lifecycle forced past the types with a cast still aborts the journey with `invalid lifecycle`, and anything else returned with `unexpected result` ([0122](0122-unexpected-results.md)).
6. **Roles.** `engine/Roles.kt` lends each role requested as a proxy that implements only that interface and forwards to the workflow object, so a hook reaches nothing else of the workflow. A role operation that throws aborts the journey with `hook failed` at once, even when the hook catches what it threw. `engine/RoleViolations.kt` refuses a role no part of the workflow provides, and a role interface itinera may not call.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/policy/PoliciesTest.kt`, `DecisionsTest.kt`, `RoleTest.kt` and `HookReporterTest.kt`: the descriptors, what each kind of hook returns, roles and the hook reporter.
- `itinera/src/test/java/org/itineradev/itinera/policy/JavaPoliciesTest.java`: policies built with lambdas from Java, and a role reached through an interface that is not public.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/HooksTest.kt`: requests, contributions, events, roles and a hook that throws.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepHooksTest.kt`: where each step hook is called and what each lifecycle does.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/LifecyclesTest.kt`: an invalid lifecycle and an unexpected result.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/PoliciesTest.kt`: a hook defined twice, and a role not provided.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: the workflow hooks before the journey's last event.
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/policy/PolicyTypesTest.kt` and `itinera-java/src/compileFailTest/java/org/itineradev/itinera/policy/JavaPolicyTypesTest.java`: the proofs of `invalid-lifecycle`, and that each hook takes only the requests its kind allows.
- `itinera/src/test/java/org/itineradev/itinera/workflow/JavaPolicyClassesTest.java` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/kotlin/KotlinPolicyClassesTest.kt`: annotated policies, and an annotated failure hook returning `OnSuccess` refused when described.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: asynchronous hooks.

## Done when

10 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #6`: every scenario tagged `@proposal-0010` passes in both runners and every column, but those that also name a proposal not listed yet, and `invalid-lifecycle` is listed as impossible.
