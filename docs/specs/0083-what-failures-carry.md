# 0083: What a failure, an abort and a refusal carry

Tech spec for [proposal 0083](https://github.com/itinera-dev/spec/blob/main/proposals/0083-what-failures-carry.md), implemented in [#28](https://github.com/itinera-dev/itinera-j/issues/28). [0065](0065-business-result.md) describes the result, and [0042](0042-configuration-errors-before-the-journey.md) the refusals.

Status: started. Stage 3 added what the result's failure and abort carry, and the refusals' errors.

## API

Until 83 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`Failure`**, in `org.itineradev.itinera.journey`, carries the `JourneyFailure` its `journey_failed` event carries, as `details`, and the error that ended the step, when an abnormal termination failed the journey. `cause`, `reason` and `errorMessage` read the details.
- **`Abort`** carries the `JourneyAbort` its `journey_aborted` event carries, as `details`, and the error that aborted the journey, when one did. `reason`, `step` and `errorMessage` read the details. Reusing the events' types keeps the result and the event saying the same thing, as itinera-ts does.
- **A refusal** caused by failing custom code carries the error and its message ([0042](0042-configuration-errors-before-the-journey.md)).
- **Error messages.** An error's message is its own; one with none gives the fixed text "an error with no message", and one whose message cannot be read, because reading it throws, gives "an error whose message could not be read", since nothing may escape `run`.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/MessagesTest.kt`: the three forms of a message.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/EngineTest.kt`: the failure and the abort carry the error the step threw.

## Done when

83 is listed in both manifests once every scenario tagged `@proposal-0083` passes in both runners.
