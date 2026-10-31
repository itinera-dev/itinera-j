# 0060: Input adapters are attached to steps, and leave unknown inputs to the data bag

Tech spec for [proposal 0060](https://github.com/itinera-dev/spec/blob/main/proposals/0060-input-adapters-attached-to-steps.md), implemented in [#21](https://github.com/itinera-dev/itinera-j/issues/21). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, sections 6 and 9 above all, [0062](0062-workflow-descriptors.md) how adapters are declared and checked, and [0091](0091-adapters-read-the-data-bag.md) their access to the data bag.

Status: started. Stage 4 declared input adapters and refused those misattached. Stage 5 calls them while a step is built. 60 is listed in stage 7, once the scenarios whose adapters see what hooks contributed pass, and the scenarios of rescue steps wait for 0003.

## Calling an adapter

`Inputs`, in `engine/Inputs.kt`, resolves each input of a step, in the order the step declares them, as chapter 4, 4.2 point 3 says:

1. The adapter attached to the step, if any, is called for each input, with the step's name and the input's key as its `Requests.stepName()` and `Requests.key()` answer them. Its own requests are resolved first, in the order it declares them: data from the workflow by `fromWorkflow`, in `engine/WorkflowData.kt`, naming the adapter in `required data missing`, `wrong type` and `optional_input_absent`, and the journey ID and its access to the data bag as asked. The first that aborts the journey stops the resolution, and the adapter is not called.
2. The adapter is called through `Parts.adapt`: `SyncParts` calls a `SyncInputAdapter`, and `AsyncParts`, in `itinera-kotlin`, awaits an `AsyncInputAdapter`, both with the workflow object.
3. `Supplied.Value` is captured, as [0056](0056-data-bag-values.md) says, emits `input_adapter_supplied`, naming the step, the attempt, the key and the adapter, and is read as the input's type, so a value of another type aborts the journey with `wrong type`, naming the adapter.
4. `Supplied.FromDataBag`, the form of nothing, leaves the input to the data bag, and a required input then missing names no adapter.
5. An adapter that throws, or supplies what is not a value, emits `input_adapter_failed` and aborts the journey with `step could not be built`, naming the step, the adapter and the key, carrying the error and its message; an `Error` is never caught.
6. An adapter that returns `null`, which only Java can write, aborts the journey with `unexpected result`, as [0122](0122-unexpected-results.md) says.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: "an input adapter supplies the input, told the step and the key, and it is announced", "an input adapter that leaves the input to the data bag reads it from there", "an input adapter that throws fails, and the step could not be built", "an input adapter that supplies what is not a value fails", "an input adapter that returns null is an unexpected result", "what an input adapter supplies of another type is wrong type, naming the adapter", "data an input adapter requires from the workflow is read first, naming the adapter" and "optional data an input adapter finds absent names the adapter".
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt`: "an asynchronous input adapter and service provider are awaited while a step is built".

## Conformance

Both runners call a scripted adapter, `Adapting`, in each runner's `run` package, which answers what the case scripts for each input and step. At the cases pinned, every scenario tagged `@proposal-0060` passes in both runners, in every column, except those that need what later stages add, contributions, hooks and rescue steps, and two that each language's types refuse: "An adapter that writes through its access leaves the data bag unchanged", which both manifests exclude as `bag-write`, as [0091](0091-adapters-read-the-data-bag.md) says, and "A synchronous-only executor refuses an asynchronous input adapter before the journey starts", tagged `mode-not-accepted`, as [0012](0012-local-executor.md) says.

## Done when

60 is listed in both manifests once every scenario tagged `@proposal-0060` passes in both runners.
