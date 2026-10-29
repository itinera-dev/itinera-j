# 0002: Foundations

Tech spec for [proposal 0002](https://github.com/itinera-dev/spec/blob/main/proposals/0002-foundations.md), implemented in [#2](https://github.com/itinera-dev/itinera-j/issues/2). [0062](0062-workflow-descriptors.md) describes the descriptors and builders, [0060](0060-input-adapters-attached-to-steps.md) the input adapters and [0010](0010-hooks-lifecycles-and-roles.md) the hooks.

Status: done. Stage 3 built steps from what they request, stage 4 declared policies, input adapters and the listing and refused what is misdeclared, stage 5 resolved inputs, and stage 7 calls hooks with the data a step contributed, and lists 2 in both manifests.

2 is listed in both manifests, so its public items carry no `@ExperimentalItineraApi`, but for `ListedStep.rescue`, the rescue section of 0003, not listed yet. `ListedStep.services`, the services of [0095](0095-services-provided-to-steps.md), kept it until stage 8 listed 95.

## How each premise is kept

1. **A step reaches only what it declares.** A step factory is called with a `Got` that answers only the requests the step declared, and an annotated step class receives only its constructor's and `run`'s parameters. A read of a request it did not declare throws, so the step could not be built. Nothing hands a step the workflow, the data bag, the executor or another step.
2. **Workflows are declarative.** A `WorkflowDescriptor` is made once, by a builder or from an annotated class, and never changes. `listing()` reads its steps in order, each with its policies, input adapter, services and rescue section, from the declaration alone, in `engine/Listings.kt`, without building or calling any part.
3. **Policies.** A step policy defines only step hooks and a workflow policy only workflow hooks: `StepPolicyDescriptor` and `WorkflowPolicyDescriptor` are separate types, and so are `@StepPolicy` and `@WorkflowPolicy`. An annotated class whose method is marked with a hook of the other kind is misdeclared. A step policy names no step and may request the step's name.
4. **Each hook at most once per target.** `engine/HooksDefinedTwice.kt` finds each hook defined by several policies of one step or one workflow. Admission reports it with every other violation, when the workflow is built or described, and again when `run` checks a hand-written descriptor ([0042](0042-configuration-errors-before-the-journey.md)).
5. **Declaration order.** A step's and a workflow's policies are kept in the order they are attached, and their hooks are called in that order.
6. **Data from the workflow, through input adapters.** `engine/Inputs.kt` resolves each input from the input adapter attached to the step and key, or else from the data bag. A missing required input aborts the journey with `required data missing`, a missing optional one is absent and `optional_input_absent` names it, and an adapter that fails or a value of another type aborts the journey ([0060](0060-input-adapters-attached-to-steps.md)).
7. **Data from the step, without adapters.** A step hook's `fromStep` and `optionalFromStep` read exactly what the step contributed in its attempt, from `ActingOn`, in `engine/ActingOn.kt`. A required key the step did not contribute aborts the journey, an optional one is absent, and a value of another type aborts the journey with `wrong type`.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: "a step whose factory reads a request it did not declare cannot be built".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ListingsTest.kt`: the listing in order, read without building or calling anything.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/PoliciesTest.kt`: a hook defined twice for one step, reported with other violations.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: inputs read from the data bag or from an adapter, absent, missing or of another type.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/HooksTest.kt`: a step hook receives what the step contributed, and an optional request for data it did not contribute is absent.

## Done when

2 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #2`: every scenario tagged `@proposal-0002` passes in both runners and every column, but those that also name a proposal not listed yet.
