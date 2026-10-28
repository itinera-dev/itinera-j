# 0102: Global metadata passed to every reporter

Tech spec for [proposal 0102](https://github.com/itinera-dev/spec/blob/main/proposals/0102-global-metadata.md), implemented in [#33](https://github.com/itinera-dev/itinera-j/issues/33). [0011](0011-events.md) describes reporters and dispatchers.

Status: started. Stage 1 added global metadata to reporters and dispatchers in both modes. Stage 3 set `journey_id` and the initial global metadata before each journey. Stage 5 lets steps set it; stage 7 lets hooks and stage 8 service providers.

## API

Until 102 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`Reporter.setGlobalMetadata(key, value)`** receives one entry: a key, and the value's captured copy, typed `Any?`. It does nothing unless overridden, so a reporter accepts every entry, as the proposal requires, and a Java class implementing `Reporter` need not write it. `AsyncReporter` has the same operation as a `suspend` function.
- **`Dispatcher.setGlobalMetadata(key, value)`** passes one entry on. `DefaultDispatcher` passes it to every reporter in the order they were added, stopping at one that throws, as for an event. `AsyncDispatcher` and `AsyncDefaultDispatcher` do the same for asynchronous workflows.

- **Before the journey**, the executor sets `journey_id`, the journey ID as a string, then each entry of the instance's initial global metadata in the order given, through the dispatcher, before `journey_started`. A reporter or the dispatcher that throws then refuses the instance with `Refused.GlobalMetadataNotSet`, with no event ([0042](0042-configuration-errors-before-the-journey.md)).

- **While the journey runs**, a step sets an entry with `setGlobalMetadata` on its step reporter ([0008](0008-steps.md)), and stage 7 and stage 8 give hooks and service providers the same operation. Each goes through `Journey.setGlobalMetadata`, in `engine/Journey.kt`, which captures a copy of the value, passes the entry to every reporter through the dispatcher, then emits `global_metadata_set`, naming the key and who set it, never the value. An entry under `journey_id` aborts the journey with `reserved metadata key`, and one that is not a value with `not a value` under its key; neither reaches a reporter. A reporter or the dispatcher that fails on the entry aborts the journey with `reporter failed`, through the same guard as an event, so the reporters after it do not receive it.

The proposal calls the reporter's operation "receive global metadata". The plan names it `setGlobalMetadata` on reporters as on dispatchers, so a reporter wrapping another passes an entry on with the call it received.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: "reporters receive each entry of global metadata in the order they were added" and "a reporter that throws stops the delivery of an entry of global metadata".
- `ReporterTest.kt`, `AsyncReporterTest.kt` and `JavaReportersTest.java`: a reporter accepts every entry unless it overrides the operation.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/JourneyTest.kt`: "an entry a step sets reaches the reporters, then global_metadata_set names its key and the step", "an entry is a copy made when it is set", "setting the journey ID aborts the journey with reserved metadata key, and no reporter receives it", "an entry that is not a value aborts the journey with not a value under its key" and "a reporter that fails on an entry aborts the journey with reporter failed".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: "the journey ID, then the initial global metadata, reach the reporters before journey_started" and "a reporter that throws on the initial global metadata refuses the instance with no event".

## Done when

102 is listed in both manifests once every scenario tagged `@proposal-0102` passes in both runners.
