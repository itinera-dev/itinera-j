# 0104: The implementation a service provider made, and its data

Tech spec for [proposal 0104](https://github.com/itinera-dev/spec/blob/main/proposals/0104-service-implementation-on-events.md), implemented in [#34](https://github.com/itinera-dev/itinera-j/issues/34). [0095](0095-services-provided-to-steps.md) says when a provider is called and `service_provided` emitted.

Status: done. Stage 5 emits `service_provided` with the implementation and the data. Stage 8 lists 104 with [0095](0095-services-provided-to-steps.md), so `ServiceProvided` and the rest of 104's API no longer carry `@ExperimentalItineraApi`.

## The implementation

`service_provided` carries the implementation as a `String`: the name of the instance's class, as `Class.getName()` gives it, such as `com.example.CardGateway`, or `com.example.Shop$CardGateway` for a nested class, as chapter 4, 4.9 point 6.3 says. The format is the JVM's, and a reporter should not depend on it.

An instance with no class of its own, made by a Kotlin or Java lambda, or by any other hidden class, has a class whose name the JVM makes up and may change from one run to the next. For such an instance, `Class.isHidden()` is true, and the implementation is the name of the type the provider declares it supplies, its `ProviderDescriptor.type`, as the specification allows. A provider cannot name the implementation itself.

The engine reads the instance's class, its name and `isHidden`, as decision 17 of the plan allows. Neither reads the class's members, so neither needs a keep rule on Android, where R8 may shorten the name of a class it is not told to keep.

## The data

`Provided.Instance(instance, data)` supplies the data, as does `provide(instance, data)` of an annotated workflow, and `Provided.Instance(instance)`, or `provide(instance)`, supplies none. The data is captured when the provider returns, as [0056](0056-data-bag-values.md) says, and `service_provided` carries the copy, `null` when there is none. Data that is not a value aborts the journey, as [0095](0095-services-provided-to-steps.md) says.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/LendingTest.kt`: "service_provided names the provider, its implementation and the data it gave", "the implementation of an instance with no class of its own is the declared service" and "data a provider gives that is not a value aborts the journey".

## Conformance

Both runners' scripted providers make instances of the implementation the case names, `first labeller` or `second labeller`, and supply the data it gives, as [0095](0095-services-provided-to-steps.md) says. At the cases pinned, every scenario tagged `@proposal-0104` passes in both runners, in every column.

## Done when

104 is listed in both manifests by the pull request of stage 8 that lists 95, 102 and 104, which says `Closes #34`: every scenario tagged `@proposal-0104` passes in both runners and every column.
