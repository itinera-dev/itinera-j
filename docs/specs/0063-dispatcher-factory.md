# 0063: The executor is given a dispatcher factory

Tech spec for [proposal 0063](https://github.com/itinera-dev/spec/blob/main/proposals/0063-dispatcher-factory.md), implemented in [#24](https://github.com/itinera-dev/itinera-j/issues/24). [0011](0011-events.md) describes reporters and dispatchers.

Status: done. Stage 1 added the factories. Stage 3 gave one to `LocalExecutor`, which calls it once before each journey and drops the dispatcher when the journey ends. Stage 6 lists 63 in both manifests.

## API

63 is listed in both manifests, and from stage 7, when 11 is listed, no public item below carries `@ExperimentalItineraApi`. Until then, each kept it, as AGENTS.md allows, since each named the dispatchers and reporters of 0011. A Kotlin class implementing `Dispatcher` or `AsyncDispatcher` opted in until stage 8 listed [0102](0102-global-metadata.md), since it overrides `setGlobalMetadata`, which belongs to 102.

- **`DispatcherFactory`**, in `org.itineradev.itinera.report`, is a `fun interface` whose `create()` returns a `Dispatcher`. A Kotlin lambda, a Java lambda or a constructor reference, such as `DefaultDispatcher::new`, is a factory.
- **`DefaultDispatcherFactory`** creates a new `DefaultDispatcher` holding no reporter each time, the default factory of chapter 2, 2.2 point 2.
- **`AsyncDispatcherFactory`** and **`AsyncDefaultDispatcherFactory`**, in `org.itineradev.itinera.report.async` in `itinera-kotlin`, are their asynchronous twins, whose `create` is a `suspend` function.

- **`LocalExecutor`** is made with a factory, or with `DefaultDispatcherFactory` when none is given ([0012](0012-local-executor.md)). A factory that throws refuses the instance with `Refused.DispatcherFactoryFailed`, one that returns `null`, which only Java code can, with `Refused.DispatcherFactoryReturnedNull`, and a dispatcher that throws while the reporters are added with `Refused.DispatcherFailed`, all before any event ([0042](0042-configuration-errors-before-the-journey.md)).

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: "the factory creates a dispatcher of its own for each journey".
- `itinera/src/test/java/org/itineradev/itinera/report/JavaReportersTest.java`: a Java lambda is a dispatcher factory.
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StartTest.kt`: a factory and a dispatcher that throw refuse the instance; `JavaExecutorTest.java`: a factory returning `null`.

## Conformance

The one scenario tagged `@proposal-0063`, "A dispatcher factory that fails is a refusal, with no event", passes in both runners and every column.

## Done when

63 is listed in both manifests by the pull request of stage 6 that lists 61 and 64, which says `Closes #24`.
