# 0042: Configuration errors are caught before a journey starts

Tech spec for [proposal 0042](https://github.com/itinera-dev/spec/blob/main/proposals/0042-configuration-errors-before-the-journey.md), implemented in [#14](https://github.com/itinera-dev/itinera-j/issues/14). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 6 and 11 above all.

Status: done. Stage 3 added the admission check of `run` and the refusals before a journey. Stage 4 added every other violation. Stage 6 lists 42 in both manifests.

## API

42 is listed in both manifests, and from stage 7 no public item below carries `@ExperimentalItineraApi`, but for what belongs to a proposal not listed yet: the violations of rescue steps (0003), of services (0095) and of global metadata (0102), and `Refused.GlobalMetadataNotSet`.

- **`Refusal`**, in `org.itineradev.itinera.executor`, is the checked exception `run` throws before any journey and any event. Its `refused` says why, as a `Refused`: `InstanceRan`, `JourneyRunning`, `Inadmissible` with every violation, `DispatcherFactoryFailed`, `DispatcherFactoryReturnedNull`, `DispatcherFailed` and `GlobalMetadataNotSet`. Those caused by failing custom code carry its error, which is also the refusal's `cause`, and its message. The executor claims the instance before anything else, so any refusal after the claim uses the instance up, and running it again is refused with `InstanceRan`, whose message says the instance was handed to an executor before.
- **`Violation`** is sealed. Stage 3 has `DuplicateStepName`, `ModeNotAccepted`, `NotAValue`, held in the initial data or the initial global metadata, `ReservedMetadataKey` and `Misdeclared`; stage 4 adds `ServiceRequestedTwice`, `ServiceNotProvided`, `StepHookDefinedTwice`, `WorkflowHookDefinedTwice` and `RoleNotProvided`. `Violations`, thrown when a workflow is described or built, lists them all.

## The order before a journey

As section 11 of the plan says: refuse a call while a journey runs, then an instance that ran; then one admission check, refused with every violation at once: those of a descriptor that `build()` did not make, which is known by its class, `Checked`; a role the descriptor provides that the workflow object does not implement; every part whose mode is not the executor's, the workflow, each step's factory, each hook's call and each reporter, whatever made them; the keys of initial data and initial global metadata whose values are not values; and initial global metadata that sets `journey_id`. Then the dispatcher is made, the guarded reporters added, and `journey_id` and the initial global metadata set. A failure in any of these is a `Refusal` with no event.

An instance that `InstanceBuilder` created carries its initial values already captured; a hand-written one has them captured by `run`.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: `journey_id` then the metadata reach the reporters first, an instance runs once, values that are not values and `journey_id` refused together with no event, initial data whose copy fails, a hand-written descriptor checked, a role the workflow object does not implement, parts of another mode, hooks included, and each failure of the dispatcher factory, the dispatcher and a reporter before the journey.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a dispatcher factory returning `null` from Java.

## Conformance

Of the eleven scenarios tagged `@proposal-0042`, "A synchronous-only executor refuses an asynchronous step before the journey starts" is excluded as impossible in both manifests, under `mode-not-accepted`, as [0012](0012-local-executor.md) says. "A configuration error in how the workflow is put together is refused before any journey, with no event" runs once 32 and 42 are listed, in the same pull request, and passes in both runners and every column. The other nine are also tagged `@proposal-0003`, so they run once 3 is listed, in stage 9.

## Done when

42 is listed in both manifests by the pull request of stage 6 that lists 12 and 32, which says `Closes #14`.
