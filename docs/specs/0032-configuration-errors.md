# 0032: Configuration errors found while running

Tech spec for [proposal 0032](https://github.com/itinera-dev/spec/blob/main/proposals/0032-configuration-errors.md), implemented in [#11](https://github.com/itinera-dev/itinera-j/issues/11). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 6, 11 and 13 above all, [0042](0042-configuration-errors-before-the-journey.md) every refusal before a journey, and [0083](0083-what-failures-carry.md) what an abort carries.

Status: done. Stage 3 added the refusals before a journey and the abort of a journey whose step cannot be built, stage 4 every violation found when a workflow is described or built, and stage 5 the aborts of a request that finds nothing or a value of another type. Stage 6 lists 32 in both manifests.

## API

32 is listed in both manifests, and from stage 7 no public item below carries `@ExperimentalItineraApi`, but for the violations of rescue steps (0003), of services (0095) and of global metadata (0102), and the details of aborts that belong to 0003, 0102 and 0122. Until then, every item kept it, as AGENTS.md allows, since `Violation` listed the violations of 0010 and 0060, and `Abort` carried the details of 0083.

- **`Violations`**, in `org.itineradev.itinera.workflow`, an unchecked exception, is what `build()`, `WorkflowDescriptor.of(...)` and `asyncDescriptorOf(...)` throw when a workflow is put together wrongly. It lists every `Violation` found at once, so a test that describes a workflow, or builds it, checks its declaration without running anything.
- **`Refusal`**, in `org.itineradev.itinera.executor`, is what `run` throws for a hand-written descriptor with a violation, with every one listed in `Refused.Inadmissible`, before any journey and any event, as [0042](0042-configuration-errors-before-the-journey.md) says.
- **`JourneyStatus.Aborted`**, in `org.itineradev.itinera.journey`, is what a configuration error found while running ends in. Its `Abort` carries the reason, the details naming the step, and the error only when custom code caused it.

## How it is built

- **Caught when described or built, whenever possible.** Every mistake that the types do not rule out and that needs no journey to find is a violation, found when the workflow is described or built, as [0042](0042-configuration-errors-before-the-journey.md) lists: no journey, no ID generator call and no event.
- **Caught while running at the latest.** What only a journey can find, such as a step that cannot be built, a required request with no value or a value of another type, records the journey's abort through `Journey.abort` and throws `JourneyAbortSignal`, which only `Engine` catches. Nothing rewrites the stream: every event already delivered stays, and `Engine` delivers `journey_aborted` last, with the step, the reason and its details, the same that the result's `Abort` carries.
- **No step runs after an abort.** The scan stops at the signal, so no `attempt_started` follows the step whose error aborted the journey.
- **No `invalid configuration`.** Proposal 0042 removed the abort reason, since every error in how a workflow is put together is refused before a journey, so `AbortReason` has none.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/workflow/WorkflowBuilderTest.kt`: two steps with one name and the other violations of the builder, refused when the workflow is built, and what a violation says.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: a hand-written descriptor checked for every violation before the journey, with no event.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: a step that cannot be built aborts the journey, naming the step and carrying the error, and no step runs after the journey is aborted, whose events stay with `journey_aborted` last.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: a required input with no value, and an input of another type, abort the journey, naming the step.

## Conformance

Of the three scenarios tagged `@proposal-0032`, "A configuration error found partway through a journey ends the stream with journey_aborted" runs once 32 is listed, and "A configuration error in how the workflow is put together is refused before any journey, with no event" once 42 is listed too, in the same pull request; both pass in both runners and every column. "An abort that no error caused carries no error" is also tagged `@proposal-0083`, so it runs from stage 7, which lists 83, and passes in both runners and every column.

## Dependencies

None.

## Done when

32 is listed in both manifests by the pull request of stage 6 that lists 12 and 42, which says `Closes #11`.
