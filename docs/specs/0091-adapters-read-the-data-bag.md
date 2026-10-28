# 0091: Input adapters may read the data bag

Tech spec for [proposal 0091](https://github.com/itinera-dev/spec/blob/main/proposals/0091-adapters-read-the-data-bag.md), implemented in [#30](https://github.com/itinera-dev/itinera-j/issues/30). [0060](0060-input-adapters-attached-to-steps.md) says how adapters are called, and [0062](0062-workflow-descriptors.md) how `DataBagAccess` is declared, and why a write through it cannot be expressed.

Status: started. Stage 4 declared the access. Stage 5 gives it to each call of an adapter that requests it. 91 is listed in stage 7, once the scenarios whose adapters see what hooks contributed pass.

## The access

For each call of an adapter, `Inputs`, in `engine/Inputs.kt`, makes a new `DataBagAccess` with `DataBags.access`, over the data bag the step is being built from, and closes it when the call returns, as chapter 4, 4.2 point 8 says:

1. It reads any key as a type the adapter names, giving the adapter its own copy, and reports a key not in the data bag as absent and a value of another type as such, through `Read`, with no event and no abort.
2. It has no method that writes, so nothing the adapter does through it changes the data bag.
3. Once its call returned, it reads every key as absent, without failing.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt`: "an input adapter reads the data bag through its access" and "an input adapter's access reads nothing once its call has returned".
- `itinera/src/test/kotlin/org/itineradev/itinera/journey/DataBagAccessTest.kt`, from stage 4: "an access reads the data bag as the data bag itself does", "an access gives the adapter its own copy, which it may change" and "once its call has returned, an access shows nothing".

## Conformance

At the cases pinned, both runners pass every scenario tagged `@proposal-0091` but those that need contributions or hooks, which later stages add, and the one that writes through the access, which both manifests exclude as `bag-write`.

## Done when

91 is listed in both manifests once every scenario tagged `@proposal-0091` passes in both runners.
