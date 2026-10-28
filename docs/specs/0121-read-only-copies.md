# 0121: Copies that keep received data read-only

Tech spec for [proposal 0121](https://github.com/itinera-dev/spec/blob/main/proposals/0121-read-only-copies.md), implemented in [#39](https://github.com/itinera-dev/itinera-j/issues/39). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 2 and 3 above all, and [0056](0056-data-bag-values.md) what a value is and how it is read.

Status: done. Built in stage 1, and listed in both manifests in stage 2, with the Kotlin runner. The proposal adds no scenario, so its tests are the Kotlin and Java tests below.

## Where itinera copies a value

Both copies go through `Values`, in the engine, which the core's own code and later stages call:

| When | Function | Application code it calls | A copy that fails |
|---|---|---|---|
| A value is captured: initial data and initial global metadata, contributions, event data, a reason's details, global metadata, an adapter's value and a provider's data | `Values.capture` | the canonical or primary constructor of each record and data class, its `init` blocks, and the `equals` and `hashCode` of a set's elements | `NotAValue`, carrying the error |
| A receiver reads a value: a request, a typed read of the data bag, or a read through an input adapter's access to the data bag | `Values.receive` | the same | `WrongType`, carrying the error; a read of the data bag gives `Read.OtherType` |

A record or data class is read through its fields, never its accessors, so no getter an application overrides is called. Copying calls only code that keeps the type and the content, so it is a copy in the sense of chapter 1, 1.3, not a value conversion, and `value-conversions` is not claimed, as section 3 of the plan says. A copy whose content differs from its original, because a constructor changes what it is given, is the application's mistake, which itinera does not detect.

`receive` checks the type first, with `isOf`, which calls no application code, so a value of another type is `WrongType` with no error, and the copy runs only for a value of the type read.

An `Error`, such as `OutOfMemoryError`, is never caught: it reaches whoever called `capture` or `receive`. Anything else thrown while copying is caught and carried.

## One copy for each receiver

Every receiver gets a fresh copy of the captured one, rebuilt from it, and no copy is shared or made read-only. A receiver may change its copy where its types allow, without changing the data bag or what any other receiver gets, as [0027](https://github.com/itinera-dev/spec/blob/main/proposals/0027-received-data-read-only.md) describes. Reporters and the result are not receivers: they get the captured copy itself, as section 2 of the plan says.

## Tests

The row of chapter 5, 5.10 for a copy that fails while a value is captured:

- in Kotlin, `itinera/src/test/kotlin/org/itineradev/itinera/engine/ValuesTest.kt`: "a copy that throws while a value is captured makes it not a value, carrying the error", for a set element's `hashCode`, and "a constructor that throws while a value is captured makes it not a value, carrying the error", for a record;
- in Kotlin, `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/value/kotlin/KotlinDataClassesTest.kt`: "a copy that throws while a data class is captured makes it not a value, carrying the error" and "a copy that throws while a set is captured makes it not a value, carrying the error";
- in Java, `itinera/src/test/java/org/itineradev/itinera/engine/JavaValuesTest.java`: `aCopyThatThrowsWhileAValueIsCapturedMakesItNotAValueCarryingTheError` and `aConstructorThatThrowsWhileAValueIsCapturedMakesItNotAValueCarryingTheError`.

The row for a copy that fails while a value is read:

- in Kotlin, `ValuesTest.kt`: "a copy that throws while a value is read makes it of the wrong type, carrying the error", and `itinera/src/test/kotlin/org/itineradev/itinera/journey/DataBagTest.kt`: "a copy that throws while the data bag is read gives another type";
- in Kotlin, `KotlinDataClassesTest.kt`: "a copy that throws while a data class is read makes it of the wrong type, carrying the error" and "a copy that throws while the data bag is read gives another type";
- in Java, `JavaValuesTest.java`: `aCopyThatThrowsWhileAValueIsReadMakesItOfTheWrongTypeCarryingTheError`, and `itinera/src/test/java/org/itineradev/itinera/journey/JavaDataBagTest.java`: `aCopyThatThrowsWhileTheDataBagIsReadGivesAnotherType`.

A copy for each receiver: "each receiver gets a copy of its own" in `ValuesTest.kt`, "each receiver gets a data class of its own" in `KotlinDataClassesTest.kt`, `eachReceiverGetsACopyOfItsOwn` in `JavaValuesTest.java`, and "a reader may change its copy without changing the data bag" in `DataBagTest.kt`.

`itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: "initial data whose copy fails is refused, listing the key and carrying the error". Stage 5 adds those of requests and adapters' reads.

## Dependencies

None of its own: kotlin-reflect, which reads data classes, belongs to [0056](0056-data-bag-values.md).

## Done when

121 is listed in both manifests by the pull request of stage 2 that adds the Kotlin runner, which says `Closes #39`.
