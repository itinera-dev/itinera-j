# 0065: The journey result is a business outcome

Tech spec for [proposal 0065](https://github.com/itinera-dev/spec/blob/main/proposals/0065-business-result.md), implemented in [#26](https://github.com/itinera-dev/itinera-j/issues/26). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 13 above all, and [0083](0083-what-failures-carry.md) what a failure and an abort carry.

Status: started. Stage 3 added the result. Stage 6 adds `output`.

## API

Until 65 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`JourneyResult`**, in `org.itineradev.itinera.journey`, a `@JvmRecord` data class, carries the journey ID and a sealed `JourneyStatus`: `Succeeded` and `Failed` with the data bag as the journey left it, `Aborted` with none, as [0085](https://github.com/itinera-dev/spec/blob/main/proposals/0085-aborted-result-has-no-data.md) requires. `isSucceeded`, `isFailed`, `isAborted`, `failure` and `abort` read it without a `when` or a `switch`; Java reads them as `isSucceeded()`, `failure()` and `abort()`, as a record's accessors.
- Nothing that happens inside a journey escapes `run` but an `Error`, so every journey that starts ends in a result.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/journey/JourneyResultTest.kt`: each status reads as itself, and the sample tells them apart.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: Java reads the result through its accessors.

## Done when

65 is listed in both manifests once every scenario tagged `@proposal-0065` passes in both runners.
