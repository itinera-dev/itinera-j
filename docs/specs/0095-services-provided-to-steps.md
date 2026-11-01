# 0095: Services provided to steps

Tech spec for [proposal 0095](https://github.com/itinera-dev/spec/blob/main/proposals/0095-services-provided-to-steps.md), implemented in [#31](https://github.com/itinera-dev/itinera-j/issues/31). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 6 and 9 above all, [0062](0062-workflow-descriptors.md) how providers are declared and checked, and [0104](0104-service-implementation-on-events.md) the implementation and data `service_provided` carries.

Status: started. Stage 4 declared providers and refused those misattached. Stage 5 calls them while a step is built and lends their instances, earlier than the plan's stage 8, since a step's services are resolved with its inputs. Stage 8 adds what a provider emits and the global metadata it sets, and 95 is listed then; the scenarios of rescue steps wait for 0003.

## Lending a service

`Lending`, in `engine/Lending.kt`, resolves each service a step requests, in the order the step declares them, after its inputs, as chapter 4, 4.9 says:

1. The provider that serves the step's key is found by `servingOf`, in `engine/Services.kt`: the one attached to the step, or else the one attached to no step.
2. **Journey scope.** A `JourneyProviderDescriptor` is called the first time an attempt of a step it serves requests its key, and `Lending` keeps the instance it made, for that journey only, and lends it to every later attempt it serves, with no further event. A journey in which no step it serves is built never calls it.
3. **Attempt scope.** An `AttemptProviderDescriptor` is called for every attempt it serves, and its instance is lent to that attempt only.
4. The provider's requests are resolved before it is called, in the order it declares them: data from the workflow by `fromWorkflow`, in `engine/WorkflowData.kt`, naming the provider in `required data missing`, `wrong type` and `optional_input_absent`, and the journey ID, and for attempt scope the step's name and the attempt, as asked. The first that aborts the journey stops the resolution, and the provider is not called.
5. The provider is called through `Parts.provide`: `SyncParts` calls a `SyncProvider`, and `AsyncParts`, in `itinera-kotlin`, awaits an `AsyncProvider`, both with the workflow object.
6. `Provided.Instance` emits `service_provided`, naming the step, the attempt, the key, the provider and the implementation, with the data captured, as [0104](0104-service-implementation-on-events.md) says, and the instance is lent. Data that is not a value aborts the journey with `not a value`, naming the step, the provider and `service_provided`, with neither event, and an instance of journey scope made in that call is never lent.
7. `Provided.NoInstance`, the form of nothing, and a provider that throws emit `service_provider_failed` and abort the journey with `step could not be built`, naming the step and the provider, with the error and its message when it threw; an `Error` is never caught.
8. A provider that returns `null`, which only Java can write, aborts the journey with `unexpected result`, as [0122](0122-unexpected-results.md) says.

### When a scope ends

itinera-j calls nothing on an instance when its scope ends, as chapter 4, 4.9 point 4.3 leaves to each language: it drops its own reference. An instance of attempt scope is held only while its attempt is built, and one of journey scope by the journey's `Lending`, which the journey drops when it ends. A service that holds a resource, such as a `Closeable`, is the workflow's to release. Keeping a service beyond its attempt can be expressed, by storing it in a field, and what it does afterwards is the workflow's business.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/LendingTest.kt`: "an instance of journey scope is made once and lent to every step it serves", "an instance of journey scope is made again for each journey", "an instance of attempt scope is made for every attempt it serves", "a provider of attempt scope is told the step and the attempt", "a provider that gives no instance fails, and the step could not be built", "a provider that throws fails, and the step could not be built", "a provider that returns null is an unexpected result", "data a provider gives that is not a value aborts the journey" and "data a provider requires from the workflow is read first, naming the provider".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuildingTest.kt`: "a step's inputs are resolved before its services".
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous input adapter and service provider are awaited while a step is built".

## Conformance

Both runners call a scripted provider, `Providing`, in each runner's `run` package, which records what it received, labels each instance it makes and fails or makes nothing as the case scripts. At the cases pinned, every scenario tagged `@proposal-0095` passes in both runners, in every column, except those that need what later layers and stages add: an attempt that fails, a step's or a provider's events, hooks and rescue steps. The scenario of a provider of journey scope requesting the attempt is refused by each language's types, with its proofs in `ProviderTypesTest.kt` and `JavaProviderTypesTest.java`, and the scenario of an asynchronous provider handed to a synchronous executor by Kotlin's, as [0012](0012-local-executor.md) says.

## Done when

95 is listed in both manifests once every scenario tagged `@proposal-0095` passes in both runners.
