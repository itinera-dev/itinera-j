# 0056: Everything in the data bag is a value

Tech spec for [proposal 0056](https://github.com/itinera-dev/spec/blob/main/proposals/0056-data-bag-values.md), implemented in [#18](https://github.com/itinera-dev/itinera-j/issues/18). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 2 above all, and [0121](0121-read-only-copies.md) when itinera copies a value.

Status: started. Stage 1 added the check and copy of a value, type tokens, the data bag and its `Read`. Stage 3 captures the initial data and the initial global metadata when the instance is created, and `run` refuses the keys whose values are not values ([0042](0042-configuration-errors-before-the-journey.md)). Later stages capture what enters a journey through the same function: adapters' values and providers' data in stage 5, as [0060](0060-input-adapters-attached-to-steps.md) and [0095](0095-services-provided-to-steps.md) say, then contributions and reasons' details later in stage 5.

## API

Until 56 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`TypeToken<T>`**, in `org.itineradev.itinera.value`: the type a read wants, generics and nullability included. `TypeToken.of(Long::class.java)` refuses `null` and `TypeToken.ofNullable(...)` admits it. In Java, `new TypeToken<List<Long>>() {}` names a generic type. In Kotlin, `typeToken<List<Long?>>()`, from `org.itineradev.itinera.value.kotlin` in `itinera-kotlin`, reads the type with `typeOf`. A token is never of a primitive: `long` gives `Long`.
- **`DataBag`**, in `org.itineradev.itinera.journey`: string keys in the order they were first set, `keys`, `contains`, and `read(key, token)`, which returns a `Read`. `itinera-kotlin` adds `data.read<Long>("amount")`, in `org.itineradev.itinera.journey.kotlin`. Applications never build a data bag; the engine does, through `DataBags.of`.
- **`Read<T>`**: `Present(value)`, a `@JvmRecord`; `Absent`; or `OtherType`, as chapter 4, 4.2 point 8.3 requires an adapter to be told. `orNull()` gives the value only when present.

Values are typed `Any?`, `Object` in Java, everywhere in the API, so no library appears in a signature.

## What a value is

`Shape.kt` decides, by an object's runtime class, how it is checked and copied, and caches the answer per class in a `ClassValue`:

1. `null`, `Boolean`, `Char`, `String`, `Byte`, `Short`, `Int`, `Long`, and `BigInteger` and `BigDecimal` of exactly those classes, since both can be subclassed, are shared.
2. A `Float` or a `Double` is a value when finite.
3. The JDK's immutable value types are shared: `UUID`, `URI`, `ZoneId` and its subclasses, and from `java.time` `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `OffsetTime`, `ZonedDateTime`, `Duration`, `Period`, `Year`, `YearMonth` and `MonthDay`. Every class but `ZoneId`'s is final.
4. An enum constant, with or without a body of its own, is shared when its fields, its enum class's and its body's, are all final and hold immutable values: shared ones, finite numbers, other such constants and `data object`s, and records and data classes holding only those. Any other enum constant is not a value, since every receiver would share what it holds. The enums of the JDK and of Kotlin's standard library, such as `ChronoUnit`, `TimeUnit`, `RoundingMode` and `DurationUnit`, are immutable and shared without reading their fields, which neither opens to itinera.
5. A `List`, a `Set` or a `Map` is copied into an `ArrayList`, a `LinkedHashSet` or a `LinkedHashMap`, keeping its order and any `null`. A map's keys must be strings.
6. A record is read through its fields and rebuilt through its canonical constructor.
7. With `itinera-kotlin`, a data class whose only fields, its superclasses' included, are the `val` properties of its primary constructor, none of a value class, is read the same way and rebuilt through its primary constructor. A `data object` whose fields and properties are all final and hold immutable values, as an enum constant's must, is shared.

Anything else is not a value, an array and a Kotlin value class included. A container that holds itself is not a value either.

A Kotlin class is recognised by its `@Metadata` annotation. The core reads it only through the `KotlinClasses` interface of its engine, which `itinera-kotlin` provides through `java.util.ServiceLoader`. Without `itinera-kotlin`, a Kotlin record is still a record, and any other Kotlin class is not a value; a type token naming one says to add `itinera-kotlin`.

### Why kotlin-reflect

`itinera-kotlin` reads data classes with kotlin-reflect, the only reader of Kotlin's metadata that tells a primary constructor's parameters from other properties, their nullability, and a property's `var`. `kotlin-metadata-jvm` reads the same metadata without loading classes, but later stages need kotlin-reflect anyway: `callSuspend` to call a `suspend` step, and the annotation readers. The core stays free of it, as the plan's "Kotlin and Java" rules ask.

## Reading by type

`isOf(value, type)`, in `TypeCheck.kt`, calls no application code. A value is of a type when `instanceof` says so, and, for a generic type:

1. the value's class is matched to the type through its supertypes, so a `Ok<Long>` is checked as an `Outcome<Long>` of a sealed interface;
2. every element of a list or set, and every value of a map, is checked against the type's argument;
3. every component of a record or data class is checked against its declared type, with the requested arguments put in.

Nothing converts: an `Int` read as a `Long` is of another type.

`null` is of a type read from Kotlin when it is nullable. A type read from Java admits `null` unless JSpecify excludes it, as below.

### What a type token may name

A token's type is checked when the token is made, and a type no value can be throws `IllegalArgumentException`, saying why. It may name:

1. any interface, such as `List`, `Collection` or a sealed interface;
2. `Object`;
3. `ArrayList`, `LinkedHashSet` and `LinkedHashMap`, the classes of a reader's copies;
4. any class whose instances are values by the rules above;
5. an abstract class with no instance fields, a sealed class of records or data classes for instance.

It may not name an array, another collection or map class, such as `LinkedList`, a map whose keys are not strings, a type variable, or any other class. The rule applies to every type argument too.

### Java's nullability through JSpecify

The readers match JSpecify's annotations by name, so the core gains no dependency:

1. A class is null-marked when its innermost mark says so, looking at the class, its enclosing method or constructor, each enclosing class, its package and its module, in that order. `@NullMarked` marks; `@NullUnmarked` unmarks.
2. A type marked `@Nullable` admits `null` and one marked `@NonNull` excludes it, in any code. In marked code any other reference type excludes it, at the top and in every type argument; in unmarked code it admits it.
3. A type variable marked `@Nullable` admits `null` whatever it is bound to, and one marked `@NonNull` excludes it. Otherwise, in marked code, it takes its nullability from the type it is bound to, as JSpecify's parametric nullness says, and in unmarked code it admits `null`.
4. A wildcard reads as its upper bound, or as any value when it has none or has a lower bound.
5. A record's components follow the same rules, read from the record's fields, with the record's own scope deciding whether it is marked.

A `TypeToken` subclass is read through `getAnnotatedSuperclass`, with the subclass's own scope deciding whether it is marked.

## Dependencies

- **kotlin-reflect**, in `itinera-kotlin` alone, at the version of the standard library, for the reason above. Apache-2.0.
- **JSpecify** 1.0.0, a test dependency of the core, for the tests of marked code.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/value/CopyTest.kt`: every shape is captured as an equal copy, every non-value refused, enum constants holding what can change among them, one named case each; copies share nothing; order and `null` are kept; a container holding itself is refused.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/ValuesTest.kt` and `itinera/src/test/java/org/itineradev/itinera/engine/JavaValuesTest.java`: what capturing and receiving carry, as [0121](0121-read-only-copies.md) lists.
- `itinera/src/test/kotlin/org/itineradev/itinera/value/TypeTokenTest.kt`: what a token accepts and refuses, one named case each.
- `itinera/src/test/kotlin/org/itineradev/itinera/journey/DataBagTest.kt` and `itinera/src/test/java/org/itineradev/itinera/journey/JavaDataBagTest.java`: present, absent, numbers that do not widen, a stored `null`, key order.
- `itinera/src/test/java/org/itineradev/itinera/value/GenericTokensTest.java` and `NullMarkedTest.java`: tokens written in Java, generic and sealed types, wildcards, and JSpecify's marks, at the top, in arguments, in record components and on type variables, by class, by enclosing class and by package.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/value/kotlin/KotlinDataClassesTest.kt`, `TypeTokensTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/journey/kotlin/ReadsTest.kt`: data classes and data objects, the Kotlin classes that are not values, Kotlin's nullability and projections, and generic data classes.

## Done when

56 is listed in both manifests once every scenario tagged `@proposal-0056` passes in both runners, in stage 6.
