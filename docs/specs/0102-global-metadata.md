# 0102: Global metadata passed to every reporter

Tech spec for [proposal 0102](https://github.com/itinera-dev/spec/blob/main/proposals/0102-global-metadata.md), implemented in [#33](https://github.com/itinera-dev/itinera-j/issues/33). [0011](0011-events.md) describes reporters and dispatchers.

Status: started. Stage 1 added global metadata to reporters and dispatchers in both modes. Stage 3 sets `journey_id` and the initial global metadata before each journey. Stage 5 lets steps set it, stage 7 hooks and stage 8 service providers.

## API

Until 102 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`Reporter.setGlobalMetadata(key, value)`** receives one entry: a key, and the value's captured copy, typed `Any?`. It does nothing unless overridden, so a reporter accepts every entry, as the proposal requires, and a Java class implementing `Reporter` need not write it. `AsyncReporter` has the same operation as a `suspend` function.
- **`Dispatcher.setGlobalMetadata(key, value)`** passes one entry on. `DefaultDispatcher` passes it to every reporter in the order they were added, stopping at one that throws, as for an event. `AsyncDispatcher` and `AsyncDefaultDispatcher` do the same for asynchronous workflows.

The proposal calls the reporter's operation "receive global metadata". The plan names it `setGlobalMetadata` on reporters as on dispatchers, so a reporter wrapping another passes an entry on with the call it received.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: "reporters receive each entry of global metadata in the order they were added" and "a reporter that throws stops the delivery of an entry of global metadata".
- `ReporterTest.kt`, `AsyncReporterTest.kt` and `JavaReportersTest.java`: a reporter accepts every entry unless it overrides the operation.

## Done when

102 is listed in both manifests once every scenario tagged `@proposal-0102` passes in both runners.
