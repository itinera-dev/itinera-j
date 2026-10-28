# 0116: Duplicate adapter names, and a step named twice in one list

Tech spec for [proposal 0116](https://github.com/itinera-dev/spec/blob/main/proposals/0116-duplicate-adapter-names.md), implemented in [#37](https://github.com/itinera-dev/itinera-j/issues/37). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 5, 6 and 9 above all, and [0062](0062-workflow-descriptors.md) the descriptors and builders these rules check.

Status: done. Built in stage 4, with the input adapters and service providers of 0062, and listed in both manifests at the end of stage 4.

## The rules and where they are checked

Every rule is checked when a workflow is built or described, by the builders' `build()`, `WorkflowDescriptor.of(...)` and `asyncDescriptorOf(...)`, which throw `Violations` listing every violation at once, and by `run` for a hand-written descriptor, as section 6 of the plan says. The engine finds them in the same functions however the workflow was declared:

- **`duplicate adapter name`.** Two input adapters of one workflow with the same `AdapterName`, whether or not they are attached to the same steps, are one `Violation.DuplicateAdapterName` naming the shared name, found by `adapterViolations` in `engine/Adapters.kt`.
- **Adapter and provider names are separate.** Adapter names are compared only with adapter names, and provider names only with provider names, so an input adapter and a service provider may share a name.
- **One adapter naming a step twice.** An adapter's steps are a list, kept in order with repetitions, so `@InputAdapter(steps = ["charge", "charge"])` or `.step("charge")` after `builder(name, "charge")` attaches it twice. Each attachment counts on its own, so the step is `Violation.StepAdaptedTwice`, naming the adapter once for each attachment, as when two adapters are attached to it.
- **One provider naming a step twice.** A provider's steps are a list too, and `providedTwice`, in `engine/ProvidedTwice.kt`, counts each attachment, so one provider naming a step twice is `Violation.ServiceProvidedTwice`, naming the key, the step and the provider once for each attachment.
- **Rescue steps.** A rescue step's name, `<step name>_rescue`, is a step name like any other in both lists, so it is adapted or provided twice in the same way.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AdaptersTest.kt`: "an adapter's shared name, a step it adapts twice and a step it names in vain are each refused", for the sample `misadapted`; "every misattached input adapter is refused when the workflow is built", with the cases "one adapter naming a step twice" and "two adapters with one name, on different steps"; and "an adapter and a service provider may have the same name".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ProvidersTest.kt`: "every misattached service provider is refused when the workflow is built", with the case "one provider naming a step twice".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/RescuesTest.kt`: "a rescue step's name adapted twice is step adapted twice" and "a rescue step's name served twice for one key is service provided twice", each with the case of one adapter or provider naming it twice.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/workflow/kotlin/KotlinAdapterMethodsTest.kt`: "annotated input adapters sharing a name and a step are refused for both" and "an annotated input adapter naming a step twice is attached to it twice".

## Conformance

The six scenarios tagged `@proposal-0116` are each tagged with a proposal that completes later, 0060, 0095 or 0003, so the selection of either runner leaves them out until the last of those is listed, as #37 says: 116 is listed at the end of stage 4, as the plan's table of stages says, before all six pass. Locally, at the cases pinned, both runners pass the five that only declare a workflow, in every column. The sixth, "An adapter and a service provider may have the same name", does not pass yet: it uses three sentences the runners define from stage 5, an adapter's value, the input a step was built with and how often a provider was called, and runs in CI once 0060 and 0095 are listed. Until then, the core's test above shows the rule it checks.

## Dependencies

None.

## Done when

116 is listed in both manifests by the pull request of stage 4 that adds the runners' sentences for input adapters, services and service providers, which says `Closes #37`.
