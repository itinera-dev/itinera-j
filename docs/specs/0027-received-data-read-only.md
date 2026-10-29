# 0027: Data a step or hook receives is read-only

Tech spec for [proposal 0027](https://github.com/itinera-dev/spec/blob/main/proposals/0027-received-data-read-only.md), implemented in [#10](https://github.com/itinera-dev/itinera-j/issues/10). [0121](0121-read-only-copies.md) says where itinera copies a value, and [0056](0056-data-bag-values.md) what a value is.

Status: done. Stage 1 made the copies. Stage 5 gives a step its own copy of everything it receives and captures each contribution when it is made. Stage 7 does the same for hooks, and lists 27 in both manifests.

## API

None. 27 is listed in both manifests.

## How it is guaranteed

The JVM cannot make a received object read-only, so itinera gives each receiver a copy of its own, as point 5 of the proposal allows:

1. **A private copy when captured.** Every value that enters a journey is checked and copied into a copy only itinera holds: the initial data, each contribution at the moment it is made, a reason's details, event data and an adapter's value. Changing the object afterwards changes nothing that was captured.
2. **A fresh copy for each receiver.** A step's inputs, data from the workflow and data from the step, a reason's details, the data bag an adapter reads, and the data bag of the result are each a new copy, so changing one changes neither the data bag nor what anyone else receives.
3. **Errors.** A hook that requests the attempt's error receives a copy of its own, written by Java serialisation and read back, or the error itself when it cannot be serialised, as the maintainer decided; `ownCopyOf`, in `engine/OwnErrors.kt`, makes it.
4. **The journey ID** is a `JourneyId`, which cannot be changed.
5. **Writing goes only through a contributor**, whose commits each emit `contribution_committed`.

## Dependencies

None of its own.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/value/CopyTest.kt` and `itinera/src/test/java/org/itineradev/itinera/value/JavaCopiesTest.java`: a copy shares nothing that can change with the original.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ContributionsTest.kt`: "a contribution is captured when it is made".
- `itinera/src/test/kotlin/org/itineradev/itinera/journey/DataBagTest.kt` and `DataBagAccessTest.kt`: a reader may change its copy without changing the data bag.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/HooksTest.kt`: "the error a hook requests is its own copy of what ended the attempt"; `OwnErrorsTest.kt`: what a copy of an error shares.

## Conformance

Every scenario tagged `@proposal-0027` passes in both runners and every column. Their sentences that change a received value in place change the receiver's own copy, as the plan's section on the runners says.

## Done when

27 is listed in both manifests by the pull request of stage 7 that lists the stage's proposals, which says `Closes #10`.
