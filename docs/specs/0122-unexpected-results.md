# 0122: Custom code that returns an unexpected result

Tech spec for [proposal 0122](https://github.com/itinera-dev/spec/blob/main/proposals/0122-unexpected-results.md), implemented in [#40](https://github.com/itinera-dev/itinera-j/issues/40). The [tier 1 plan](../tier-1-plan.md) holds what crosses proposals, section 11 above all.

Status: started. Stage 3 added the calls below. Each later stage adds the calls it brings, and stage 9 lists 122.

## The calls stage 3 adds

Kotlin's types rule out every `null` below, but a Java class or a Java lambda can return one, and Kotlin does not check what Java code returns, so the engine checks each result.

| Call | In Kotlin | In Java | Its form of nothing | A `null` |
|---|---|---|---|---|
| A step's factory, `SyncStepFactory.build` | `fun build(): SyncStepRun` | `SyncStepRun build()` | none | aborts with `unexpected result`, `ByStep`: "the step's factory returned null, not a step" |
| A step's run, `SyncStepRun.run`, or an annotated step's `run` | `fun run(): Outcome` | `Outcome run()` | none | aborts with `unexpected result`, `ByStep`: "the step's run returned null, not an outcome" |
| The dispatcher factory, `DispatcherFactory.create` | `fun create(): Dispatcher` | `Dispatcher create()` | none | refused with `Refused.DispatcherFactoryReturnedNull` |
| The ID generator, `JourneyIdGenerator.generate` or an `@IdGenerator` method | `fun generate(workflow: W, data: DataBag): String` | `String generate(W workflow, DataBag data)` | none | `create()` throws `IllegalStateException`, as for an empty ID |
| A reporter factory, `ReporterFactory.create` | `fun create(journeyId: JourneyId): Reporter` | `Reporter create(JourneyId journeyId)` | none | `create()` throws `IllegalStateException` |

The ID generator and the reporter factories are called while the instance is created, before any journey, so a `null` from them is a failure of `create()`, as chapter 5, 5.1 point 1 says of any failure then, not a refusal of `run`. Reporters and dispatchers are void ([0011](0011-events.md)).

An unexpected result is neither an outcome nor an abnormal termination, so it is never retried, and the abort carries no error.

## Dependencies

None.

## Tests

- `itinera/src/test/java/org/itineradev/itinera/executor/JavaExecutorTest.java`: a step factory and a run returning `null`, a dispatcher factory returning `null`, an ID generator and a reporter factory returning `null`.

## Done when

122 is listed in both manifests once every scenario tagged `@proposal-0122` passes in both runners.
