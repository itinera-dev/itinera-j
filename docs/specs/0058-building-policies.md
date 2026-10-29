# 0058: Workflow policies are built per journey, step policies per attempt

Tech spec for [proposal 0058](https://github.com/itinera-dev/spec/blob/main/proposals/0058-building-policies.md), implemented in [#20](https://github.com/itinera-dev/itinera-j/issues/20). [0010](0010-hooks-lifecycles-and-roles.md) describes policies and hooks, and [0049](0049-custom-code-that-throws.md) what a failure while building one does.

Status: done. Stage 4 declared each policy with the factory that builds it, and the hooks it defines fixed by its descriptor. Stage 7 builds the policies as a journey runs, and lists 58 in both manifests.

## API

58 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`.

- **`PolicyFactory<P>`**, in `org.itineradev.itinera.policy`, builds a policy; `StepPolicyDescriptor.builder(name, factory)` and `WorkflowPolicyDescriptor.builder(name, factory)` take one, and an annotated policy class is built with its constructor that takes nothing.
- **`PolicyCouldNotBeBuilt`**, in `org.itineradev.itinera.event`, the details of the abort `policy could not be built`, naming the step and the policy.
- **`Refused.WorkflowPolicyFailed`** and **`Refused.WorkflowPolicyReturnedNull`**, in `org.itineradev.itinera.executor`, the refusals of a workflow policy that cannot be built.

## How it is built

1. **Definitions are shared, instances are not.** A descriptor holds the policy's name, its factory and the hooks it defines, so `hook defined twice` is found when the workflow is built or described, and building never changes which hooks a policy defines.
2. **Workflow policies, once per journey.** `workflowPolicies`, in `engine/BuiltPolicies.kt`, builds each workflow policy once, in the order attached, before the dispatcher is made. One that throws refuses the instance with `Refused.WorkflowPolicyFailed`, and one built as `null`, which only Java code can, with `Refused.WorkflowPolicyReturnedNull`, both with no event.
3. **Step policies, for every attempt.** `stepPolicies` builds each step policy attached to the step anew right after `attempt_started`, before the step's requests are resolved, and every hook called for that attempt, `on step failure` after `step_given_up` included, uses those instances. A step policy attached to two steps gets one instance for each. One that throws aborts the journey with `policy could not be built`, naming the step and the policy, and one built as `null` with `unexpected result`.
4. **Read-only policies.** The JVM cannot make an object read-only inside a method, so itinera cannot stop a hook from changing its policy's fields. Points 2 and 3 still keep any such change from outliving the journey, for a workflow policy, or the attempt, for a step policy, as the documentation of `PolicyFactory`, `ItineraStepPolicy` and `ItineraWorkflowPolicy` says.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuiltPoliciesTest.kt`: a step policy built anew for each attempt and shared by its hooks, a workflow policy built once per journey, and each policy that throws or is built as `null`.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/PoliciesTest.kt`: a hook defined twice, found when the workflow is built.

## Conformance

Every scenario tagged `@proposal-0058` passes in both runners and every column.

## Done when

58 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #20`.
