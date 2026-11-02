# 0065: The journey result is a business outcome

Tech spec for [proposal 0065](https://github.com/itinera-dev/spec/blob/main/proposals/0065-business-result.md), implemented in [#26](https://github.com/itinera-dev/itinera-j/issues/26). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 13 above all, and [0083](0083-what-failures-carry.md) what a failure and an abort carry.

Status: done. Stage 3 added the result. Stage 6 adds `output`, initial data given as a record or data class, and lists 65 in both manifests.

## API

65 is listed in both manifests, and no public item below carries `@ExperimentalItineraApi`. Until stage 7 listed [0083](0083-what-failures-carry.md), `JourneyResult` and `JourneyStatus` kept it, as AGENTS.md allows, since they name `Failure` and `Abort`, which carry 0083's details.

- **`JourneyResult`**, in `org.itineradev.itinera.journey`, a `@JvmRecord` data class, carries the journey ID and a sealed `JourneyStatus`: `Succeeded` and `Failed` with the data bag as the journey left it, `Aborted` with none, as [0085](https://github.com/itinera-dev/spec/blob/main/proposals/0085-aborted-result-has-no-data.md) requires. `isSucceeded`, `isFailed`, `isAborted`, `failure` and `abort` read it without a `when` or a `switch`; Java reads them as `isSucceeded()`, `failure()` and `abort()`, as a record's accessors.
- **`output(type)`** on `JourneyResult` reads the data bag of a journey that succeeded or failed as the record or data class a `TypeToken` names, the counterpart of Rust's `#[derive(Output)]`; `output<T>()` in `org.itineradev.itinera.journey.kotlin`, in `itinera-kotlin`, reads it as a reified Kotlin type, generics and nullability included. Each component is the reader's own copy of the value under its key, read as the component's declared type, with the token's type arguments put in. Keys no component names are left unread.
- **`OutputException`**, unchecked, is what `output` throws when it cannot read, with a sealed `OutputProblem`: `Aborted`, since an aborted journey has no data bag, `Absent(key)` for a required component whose key holds nothing, and `OtherType(key)` for a value of another type than the component's. It is a throw rather than a returned result, since a Java caller would otherwise unwrap every read, and an application that reads its own output knows its shape. `output` throws `IllegalArgumentException` for a type that is not a record or a data class, as for a Kotlin data class without `itinera-kotlin`.
- **`initialData(data)`**, on `InstanceBuilder` and `AsyncInstanceBuilder`, beside `initialData(map)`, takes a record or data class and puts each component in the initial data under its key, in the components' order, the counterpart of Rust's `#[derive(InitialData)]`. It throws `IllegalArgumentException` at once for anything else, a misuse the types cannot rule out.
- **`@Datum(key, required)`**, in `org.itineradev.itinera.journey`, both arguments optional, marks a component of either: `key` names its key, which is otherwise the component's name, and `required` makes a component whose type admits `null` required, as `@Input(required = true)` does for an input. It targets parameters and fields: on a Kotlin constructor property it lands on the constructor's parameter, and on a Java record component on both the canonical constructor's parameter and the field, or the field alone when the canonical constructor is written out, so the reader looks at the parameter, then the field. An `Optional` component marked required is refused, since an `Optional` is always optional.
- **Required or optional.** A component is optional exactly as a request is: of a nullable type in a Kotlin class that `itinera-kotlin` reads, and an `Optional` in Java, which needs `itinera-java`, as an `Optional` input does. Without `itinera-kotlin`, a Kotlin `@JvmRecord` is read as a Java record, since Java reflection cannot see Kotlin's nullability: only an `Optional` component is optional. An optional component gives no key when it holds `null` or an empty `Optional`, and takes `null`, or an empty `Optional`, when its key holds nothing. Any other component is required: it always gives its key, `null` included, and `output` needs its key. A Java component that is not an `Optional` is therefore required, whether or not JSpecify lets it hold `null`.
- Nothing that happens inside a journey escapes `run` but an `Error`, so every journey that starts ends in a result.

## How it is built

`engine/JourneyData.kt` is an annotation reader, one of the places AGENTS.md allows reflection in. It reads a record or data class once per call through the shape the value walk already gives it, `Shape.Composite`: its components, the fields holding them, and the canonical or primary constructor. Beyond that shape it reads only `@Datum`, and each component's value through the walk's fields. Each component's constructor parameter, or else its field, gives its `@Datum`; `WrittenRequest`, which the readers of steps and hooks use, decides from its declared type and `required` whether it is optional and what type its value is read as; `Values.receive` reads each value, and the constructor builds the output, throwing what it throws. Two components giving one key, an `Optional` component marked required, or a component of a type no value can be, are refused with `IllegalArgumentException` before anything is read.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/journey/JourneyResultTest.kt`: each status reads as itself, the sample tells them apart, and the output sample reads the data bag of a succeeded and of a failed journey and says why it cannot, a Kotlin record read without `itinera-kotlin` included.
- `itinera/src/test/java/org/itineradev/itinera/journey/JavaJourneyResultTest.java`: Java reads why an aborted journey has no output.
- `itinera/src/test/java/org/itineradev/itinera/engine/JavaJourneyDataTest.java`: Java records read as output, keyed by name or by `@Datum`, on the parameter or, with a canonical constructor written out, on the field, the problems `output` reports, the types it refuses, and records given as initial data, in order, keyed by `@Datum`, a component that is not an `Optional` giving its `null`.
- `itinera/src/test/kotlin/org/itineradev/itinera/instance/InstanceBuilderTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/instance/async/AsyncInstanceBuilderTest.kt`: the samples giving initial data as a record and as a data class.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/journey/kotlin/KotlinJourneyDataTest.kt`: Kotlin's nullability in data classes, a generic data class read with its type argument, and a nullable component marked required, both as output and as initial data.
- `itinera-java/src/test/java/org/itineradev/itinera/request/java/JavaOptionalJourneyDataTest.java`: an `Optional` component, empty and holding a value, given as initial data and read back as output.
- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: Java reads the result through its accessors.

## Conformance

The one scenario tagged `@proposal-0065`, "A failed journey's result carries its ID and the reason that failed it", runs once 65 is listed, and passes in both runners and every column. `output` and `@Datum` are not observable by the conformance suite.

## Done when

65 is listed in both manifests by the pull request of stage 6 that adds `output`, which says `Closes #26`.
