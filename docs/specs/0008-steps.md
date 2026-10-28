# 0008: Steps

Tech spec for [proposal 0008](https://github.com/itinera-dev/spec/blob/main/proposals/0008-steps.md), implemented in [#4](https://github.com/itinera-dev/itinera-j/issues/4). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 4 and 7 above all, and [0062](0062-workflow-descriptors.md) the step descriptors and how steps declare what they request.

Status: started. Stage 3 built a step for each attempt and ran it, and stage 4 let it declare what it requests. Stage 5 resolves those requests before each attempt is built, then adds the outcomes, contributions and handles.

## Building a step for each attempt

Every attempt of a step is built anew, by its factory, in `engine/Attempts.kt`. Before the factory is called, `Building`, in `engine/Building.kt`, resolves every request the step declared, as chapter 4, 4.2 says, so a built step has all its inputs and services and a read never waits:

1. **Inputs first**, in the order the step declares them, by `Inputs`, in `engine/Inputs.kt`: through the step's input adapter, as [0060](0060-input-adapters-attached-to-steps.md) says, or from the data bag, by `fromWorkflow`, in `engine/WorkflowData.kt`. A required input with no value aborts the journey with `required data missing`, naming the step and the key; an optional one is absent, `null` in Kotlin and an empty `Optional` through `itinera-java`'s `JavaRequests.optionalInput`, and emits `optional_input_absent`, naming the step and the attempt. A value of another type aborts the journey with `wrong type`, naming the step and the key. Each input is the step's own copy, as [0121](0121-read-only-copies.md) says.
2. **Services next**, in the order the step declares them, by `Lending`, in `engine/Lending.kt`, as [0095](0095-services-provided-to-steps.md) says. A step whose inputs abort the journey never causes a provider to be called.
3. **Then the rest**: the journey ID, the step's name and the attempt are answered as asked, by `Call`, in `engine/Call.kt`.

The answers reach the factory as an `Answered`, in `engine/Answered.kt`, the engine's `Got`: reading a request the step did not declare throws `IllegalStateException`, which the engine catches as any failure of the factory, so the step could not be built. Until stage 5 makes the handles, reading one throws in the same way, saying this version does not resolve it yet.

A step that cannot be built aborts the journey with `step could not be built`, and never enters the retry loop, as chapter 4, 4.2 point 7 says.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: "a step reads its inputs from the data bag", "an optional input with no value is absent, and optional_input_absent names the step", "a required input with no value aborts the journey before the step is built" and "an input of another type aborts the journey with wrong type, naming the step".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuildingTest.kt`: "a step's inputs are resolved before its services".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: "a step whose factory reads a request it did not declare cannot be built".
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous input adapter and service provider are awaited while a step is built".

## Done when

8 is listed in both manifests once every scenario tagged `@proposal-0008` passes in both runners.
