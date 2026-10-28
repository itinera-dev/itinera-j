# 0063: The executor is given a dispatcher factory

Tech spec for [proposal 0063](https://github.com/itinera-dev/spec/blob/main/proposals/0063-dispatcher-factory.md), implemented in [#24](https://github.com/itinera-dev/itinera-j/issues/24). [0011](0011-events.md) describes reporters and dispatchers.

Status: started. Stage 1 added the factories. Stage 3 gave one to `LocalExecutor`, which calls it once before each journey and drops the dispatcher when the journey ends.

## API

Until 63 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

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

## Done when

63 is listed in both manifests once every scenario tagged `@proposal-0063` passes in both runners.
