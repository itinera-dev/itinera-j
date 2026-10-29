# 0098: Input adapters and service providers for rescue steps

Tech spec for [proposal 0098](https://github.com/itinera-dev/spec/blob/main/proposals/0098-rescue-step-adapters-and-providers.md), implemented in [#32](https://github.com/itinera-dev/itinera-j/issues/32). It amends [0003](0003-rescue-steps.md), [0060](0060-input-adapters-attached-to-steps.md) and [0095](0095-services-provided-to-steps.md). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, and [0062](0062-workflow-descriptors.md) how adapters, providers and rescue steps are declared and checked.

Status: started. Stage 4 checked the attachments and listed each rescue step's adapter and services; stage 9 resolves a rescue step's inputs and services through them, and its last pull request lists 98 in both manifests.

## Attaching to a rescue step

1. **Its name.** A rescue step's name, `<step name>_rescue`, which `rescueOf` in `engine/Rescues.kt` derives, counts among the workflow's steps while its step has a rescue step, so an adapter or a provider may be attached to it as to a declared step, with the builders' `step(name)` or with `@InputAdapter(steps = [...])` and `@Provider(steps = [...])`.
2. **Only while the step has one.** Attaching to it otherwise is `input adapter for unknown step` or `service provider for unknown step`, as for any name the workflow does not have; two adapters attached to it are `step adapted twice`, and two providers of one key `service provided twice`, as [0116](0116-duplicate-adapter-names.md) counts them.
3. **The listing.** `ListedRescue` holds the rescue step's name, the name of its input adapter or `null`, and its services, each with the provider that serves it, chosen as the executor chooses it.

## Building a rescue step

1. **Inputs.** `Building.rescuing`, in `engine/Building.kt`, resolves each input through `Inputs`, by the rescue step's own name and attempt 1, so only the adapter attached to `<step name>_rescue` is asked, never its step's, and an input it leaves comes from the data bag. An adapter that requests the name of the step being built receives the rescue step's name. The events and aborts are those of any adapted step, naming the rescue step.
2. **Never the adapter for what the step left.** What the rescue step requests from the step it rescues, its contributions, the reason and the error, is resolved from what the failed attempt left, as for a step hook, and never asked of an adapter.
3. **Services.** `Lending`, in `engine/Lending.kt`, lends each service by the rescue step's name, so the provider of the key attached to `<step name>_rescue` serves it, or else the one attached to no step, never its step's. A provider of journey scope attached to a step and to its rescue step lends both its one instance, and a provider of attempt scope receives the rescue step's name and attempt 1.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/RescuesTest.kt`: the attachments refused and allowed, since stage 4.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuildingTest.kt`: "a rescue step's inputs come through the adapter attached to its own name, never its step's" and "a rescue step's service comes from the provider attached to its own name, never its step's".

## Done when

98 is listed in both manifests once every scenario tagged `@proposal-0098` passes in both runners and every column.
