# 0085: An aborted journey's result carries no data bag, and an abort is final

Tech spec for [proposal 0085](https://github.com/itinera-dev/spec/blob/main/proposals/0085-aborted-result-has-no-data.md), implemented in [#29](https://github.com/itinera-dev/itinera-j/issues/29). [0065](0065-business-result.md) describes the result, and [0083](0083-what-failures-carry.md) what an abort carries.

Status: done. Stage 3 gave an aborted result no data bag. Stage 6 made `output` throw for an aborted journey. Stage 7 runs the scenarios whose aborts come from hooks and policies, and lists 85 in both manifests.

## API

85 is listed in both manifests, and none of its public items carries `@ExperimentalItineraApi`.

- **`JourneyStatus.Aborted`**, in `org.itineradev.itinera.journey`, holds only the `Abort`: its reason, its details and the error when custom code caused it. `Succeeded` and `Failed` hold the data bag; `Aborted` has none to hold, so no code can read one.
- **`JourneyResult.output`** throws `OutputException` with `OutputProblem.Aborted` for an aborted journey.

## How it is built

1. The engine records the first abort, and once the journey is aborted it emits nothing but `journey_aborted`, commits nothing more, and runs no step or hook.
2. When a reporter fails on a `contribution_committed`, whether the remaining contributions are committed is not observable, since the result carries no data bag; the engine stops committing at the abort.
3. An instance runs once, and `run` refuses it again with `Refused.InstanceRan`, so an aborted journey is never resumed.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/journey/JourneyResultTest.kt` and `itinera/src/test/java/org/itineradev/itinera/journey/JavaJourneyResultTest.java`: an aborted journey reads as aborted, with its abort, and its `output` says why it cannot be read.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: "an instance runs once".

## Conformance

Every scenario tagged `@proposal-0085` passes in both runners and every column.

## Done when

85 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #29`.
