# 0122: Custom code that returns an unexpected result

Tech spec for [proposal 0122](https://github.com/itinera-dev/spec/blob/main/proposals/0122-unexpected-results.md), implemented in [#40](https://github.com/itinera-dev/itinera-j/issues/40). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 11 above all.

Status: done. Stage 3 added the calls below, stage 5 those of input adapters and service providers, stage 7 those of policies and hooks, and stage 9 those of rescue steps. Stage 9 lists 122 in both manifests, so `UnexpectedResult` and `AbortReason.UNEXPECTED_RESULT` no longer carry `@ExperimentalItineraApi`.

## The calls stage 3 adds

Kotlin's types rule out every `null` below, but a Java class or a Java lambda can return one, and Kotlin does not check what Java code returns, so the engine checks each result.

| Call | In Kotlin | In Java | Its form of nothing | A `null` |
|---|---|---|---|---|
| A step's factory, `SyncStepFactory.build` | `fun build(): SyncStepRun` | `SyncStepRun build()` | none | aborts with `unexpected result`, `ByStep`: "the step's factory returned null, not a step" |
| A step's run, `SyncStepRun.run`, or an annotated step's `run` | `fun run(): Outcome` | `Outcome run()` | none | aborts with `unexpected result`, `ByStep`: "the step's run returned null, not an outcome" |
| The dispatcher factory, `DispatcherFactory.create` | `fun create(): Dispatcher` | `Dispatcher create()` | none | refused with `Refused.DispatcherFactoryReturnedNull` |
| The ID generator, `JourneyIdGenerator.generate` or an `@IdGenerator` method | `fun generate(workflow: W, data: DataBag): String` | `String generate(W workflow, DataBag data)` | none | `create()` throws `IllegalStateException`, as for an empty ID |
| A reporter factory, `ReporterFactory.create` | `fun create(journeyId: JourneyId): Reporter` | `Reporter create(JourneyId journeyId)` | none | `create()` throws `IllegalStateException` |

## The calls stage 5 adds

| Call | In Kotlin | In Java | Its form of nothing | A `null` |
|---|---|---|---|---|
| An input adapter, `SyncInputAdapter.adapt` or an `@InputAdapter` method | `fun adapt(workflow: W, got: Got<InputAdapterKind>): Supplied` | `Supplied adapt(W workflow, Got<InputAdapterKind> got)` | `Supplied.FromDataBag` | aborts with `unexpected result`, `ByAdapterForKey`: "the input adapter returned null, not what it supplies" |
| A service provider, `SyncProvider.provide` or a `@Provider` method | `fun provide(workflow: W, got: Got<K>): Provided<T>` | `Provided<T> provide(W workflow, Got<K> got)` | `Provided.NoInstance` | aborts with `unexpected result`, `ByProviderForKey`: "the service provider returned null, not what it provides" |

Neither emits an event before the abort: not `input_adapter_supplied` nor `input_adapter_failed`, not `service_provided` nor `service_provider_failed`, as chapter 4, 4.2 point 3.1 and 4.9 point 6.5 say.

## The calls stage 7 adds

| Call | In Kotlin | In Java | Its form of nothing | A `null` |
|---|---|---|---|---|
| A policy's factory, `PolicyFactory.build`, for a step policy | `fun build(): P`, with `P : Any` | `P build()` | none | aborts with `unexpected result`, `ByPolicy`: "building the policy … gave null, not the policy" |
| A policy's factory, for a workflow policy | the same | the same | none | refused with `Refused.WorkflowPolicyReturnedNull` |
| A step hook, `SyncHook.call` or an annotated hook | `fun call(policy: P, got: Got<K>): R`, `R` being `OnSuccess` or `OnFailure` | the same, which an unchecked cast can make return anything | `keepDefault()` | aborts with `unexpected result`, `ByStepHook`, for `null` and for anything that is neither `keepDefault()` nor a lifecycle: "the hook … returned null, not keepDefault() or a lifecycle", or "an object of" its class |

A workflow hook, `SyncWorkflowHook.call`, returns `Unit` in Kotlin and is `void` in Java, so what it returns is never read.

## The calls stage 9 adds

| Call | In Kotlin | In Java | Its form of nothing | A `null` |
|---|---|---|---|---|
| A rescue step's factory, `SyncRescueFactory.build` | `fun build(got: Got<RescueKind<Sync>>): SyncRescueRun` | `SyncRescueRun build(Got<RescueKind<Sync>> got)` | none | aborts with `unexpected result`, `ByStep`, naming the rescue step: "the rescue step's factory returned null, not a rescue step" |
| A rescue step's run, `SyncRescueRun.run`, or an annotated rescue step's `run` | `fun run(): RescueOutcome` | `RescueOutcome run()` | none | aborts with `unexpected result`, `ByStep`, naming the rescue step: "the rescue step's run returned null, not an outcome" |

## When the calls are made

The ID generator and the reporter factories are called while the instance is created, before any journey, so a `null` from them is a failure of `create()`, as chapter 5, 5.1 point 1 says of any failure then, not a refusal of `run`. Reporters and dispatchers are void ([0011](0011-events.md)).

An unexpected result is neither an outcome nor an abnormal termination, so it is never retried, and the abort carries no error.

In Kotlin, compile-fail tests show that none of the calls above can return `null`, nor the asynchronous step's factory and run, nor the asynchronous factories of reporters and dispatchers, that a step hook cannot return anything but `keepDefault()` or a lifecycle, and that an ID generator cannot return anything but a string. A policy's type is bounded by `Any`, on `PolicyFactory`, the policy descriptors and their builders, since a policy that admits `null` would let a Kotlin factory return it. Annotated Kotlin code whose return type admits `null` is refused when described, as [0062](0062-workflow-descriptors.md) says. The Kotlin manifest lists `non-string-journey-id` under `impossible`, proven by `BuilderTypesTest.kt`, and `unexpected-result`, each scenario proven by the test of its call.

The Java runner runs every scenario of both tags but the asynchronous one: it scripts a `null` from each call, and a step hook that returns a string through an unchecked cast. The Kotlin runner leaves their sentences undefined, since Kotlin cannot write them.

## Dependencies

None.

## Tests

- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a step factory and a run returning `null`, a dispatcher factory returning `null`, an ID generator and a reporter factory returning `null`, and an input adapter and a service provider returning `null`, and a rescue step's factory and run returning `null`.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: "a rescue step that gives null is an unexpected result naming it", from its factory and from its run.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/InputsTest.kt` and `LendingTest.kt`: "an input adapter that returns null is an unexpected result" and "a provider that returns null is an unexpected result", through an unchecked `null` as Java returns it.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/BuiltPoliciesTest.kt`: "a step policy built as null aborts the journey with unexpected result" and "a workflow policy built as null refuses the instance", through an unchecked `null` as Java returns it.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/LifecyclesTest.kt`: "what a step hook returns that is no lifecycle aborts the journey with unexpected result".
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/workflow/RescueTypesTest.kt`, `policy/PolicyTypesTest.kt`, `workflow/AdapterTypesTest.kt` and `service/ProviderTypesTest.kt`: compile-fail tests, each beside a twin that compiles: a rescue step's factory and run, a policy's factory, a step hook, an input adapter and a service provider never return `null`, and a step hook nothing but `keepDefault()` or a lifecycle.
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/workflow/BuilderTypesTest.kt`: compile-fail tests, each beside a twin that compiles in the matching `Twins` file: Kotlin cannot write a step factory or a run returning `null`, nor an ID generator returning `null` or a number.
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/report/ReportTypesTest.kt` and `itinera-kotlin/src/compileFailTest/kotlin/org/itineradev/itinera/report/async/AsyncReportTypesTest.kt`: compile-fail tests, each beside a twin that compiles in the matching `Twins` file: no reporter factory or dispatcher factory, of either mode, returns `null`.
- `itinera-kotlin/src/compileFailTest/kotlin/org/itineradev/itinera/step/async/AsyncStepTypesTest.kt`: the same for an asynchronous step's factory and run.

## Done when

122 is listed in both manifests once every scenario tagged `@proposal-0122` passes in both runners, but those the Kotlin manifest excludes as `unexpected-result`.
