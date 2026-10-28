# 0011: Events

Tech spec for [proposal 0011](https://github.com/itinera-dev/spec/blob/main/proposals/0011-events.md), implemented in [#7](https://github.com/itinera-dev/itinera-j/issues/7). The [tier 1 plan](../tier-1-plan.md), section 12, holds what crosses proposals; [0040](0040-events-facts-and-decisions.md) covers decisions and [0064](0064-event-data-values.md) the data events carry.

Status: started. Stage 1 added the events and what they carry, and reporters, dispatchers and their factories in both modes. From stage 3 the engine emits them, numbered from 1 and stamped by its clock, and its guard around each reporter keeps the rules of chapter 2, 2.3 whatever the dispatcher ([0081](0081-reporter-failure-stops-delivery.md)). From stage 5 steps emit their own events through their step reporter ([0008](0008-steps.md)).

This tech spec covers every type of chapter 2 as amended, including the event types that belong to other proposals: the rescue's events and causes (0003), what failures and aborts carry (0083), the service events (0095 and 0104), `global_metadata_set` and `reserved metadata key` (0102), and `unexpected result` (0122). Each of those proposals gets its own tech spec in the stage that builds its behaviour.

## API

Until 11 is listed in both manifests, every public item below carries `@ExperimentalItineraApi`.

- **`Event`**, in `org.itineradev.itinera.event`, is a sealed interface with one `@JvmRecord` data class per kind of chapter 2, 2.7 and 2.9, such as `StepFailed` or `JourneyAborted`. Kotlin matches an event with `when`, and Java with `instanceof`.
- `hook_called` is the one kind with two classes, under the sealed interface `HookCalled`. `StepHookCalled` carries the lifecycle a step hook returned, or `null` when it kept the default. `WorkflowHookCalled` carries none, since a workflow hook returns no lifecycle.
- **Fixed fields.** Every event carries `kind`, an `EventKind`; `sequence`, a `Long` counted from 1; `timestamp`, an `Instant`, which prints in ISO 8601 in UTC; `journeyId`; and `workflow`, the workflow's name.
- **`EventKind`** is an enum. Each member prints as the specification names the kind, in snake_case, and `EventKind.parse` reads it back, as chapter 2, 2.13 suggests.
- **Names**: `WorkflowName` and `AdapterName` in `workflow`, `StepName` in `step`, `PolicyName` in `policy` and `ProviderName` in `service`. Each is a small final class made with `of`, which refuses an empty string, as the plan's first rule for Java asks. They share the abstract class `Name`, so two names are equal only when they are of the same class and spell the same text.
- **`JourneyId`**, in `journey`, is made with `JourneyId.of`, which refuses an empty string, as chapter 5, 5.2 and proposal 0122 require.
- **`Attempt`**, in `step`, is made with `Attempt.of`, which refuses a number below 1. It has `number` and `isFirst`, and attempts compare by their numbers.
- **`Reason`**, in `step`, is a `@JvmRecord` with a code, an optional message and optional details, a value. Its constructor has Java overloads for a code alone and for a code and a message.
- **Who did what.** Two families of types say who an event or an abort is about:
  - the sources an event carries, each with the step and attempt where there is one: `StepSource`, `AdapterSource`, `StepHookSource`, `ProviderSource` and `ByWorkflowHook`, under the sealed interfaces `RequestSource`, `ContributionSource`, `MetadataSource`, `EmitterSource` and `HookSource`, one for each field that accepts only some of them;
  - the `By` types an abort carries, without a step, since the abort names its own: `ByStep`, `ByRescueStep`, `ByAdapter`, `ByAdapterForKey`, `ByStepHook`, `ByWorkflowHook`, `ByProvider`, `ByProviderForKey` and `ByPolicy`, under `Requester`, `FailureRequester`, `BuildSource`, `ValueSource` and `ResultSource`.
- **`JourneyAbort`** is a sealed interface with one record per abort reason, carrying exactly what chapter 2, 2.7 lists for `journey_aborted`. `not a value` has three forms under `NotAValue`: under a key, in an event, or in a reason's details. Each abort gives its `reason`, an `AbortReason`; its `step`, if any; and the `errorMessage` when failing custom code caused it. A `wrong type` or `not a value` abort carries one only when copying the value called application code that threw, a constructor or a set element's `hashCode`, as [0121](0121-read-only-copies.md) and section 2 of the plan say. A reason's details are given only by a step, a rescue step included, or by a step hook returning `FailWorkflow`, so `NotAValue.InDetails` names its source as a `ReasonSource`.
- **Closed sets** are enums whose members print as the specification writes them: `StepHook`, `WorkflowHook` and `RetryCause` in `policy`, `AbortReason` and `FailureCause` in `journey`, and `RescueCause` in `event`.

## Reporters and dispatchers

Every type below is in `org.itineradev.itinera.report` for synchronous workflows, and in `org.itineradev.itinera.report.async`, in `itinera-kotlin`, for asynchronous ones, where each name begins with `Async` and each operation is a `suspend` function marked `@JvmSynthetic`, so Java never sees it. javac still accepts a Java class that implements one of these interfaces and writes none of its operations, since it cannot see them; calling one would throw `AbstractMethodError`. Such a class is refused as a part of the other mode, as the plan's section on how each language meets only its own part says: `AsyncLocalExecutor.run` refuses an instance holding such a reporter with `mode not accepted`, and `AsyncLocalExecutor`'s constructor refuses such a dispatcher factory with `IllegalArgumentException`, since a reporter joins an instance and a dispatcher factory an executor, never a workflow. A class counts as written in Kotlin when it carries Kotlin's `@Metadata` or is a lambda's hidden class, so a mock that Mockito or MockK generates, and a `java.lang.reflect.Proxy`, are refused too; tests stand in for an asynchronous reporter with a Kotlin class or lambda.

- **`Reporter`** is a `fun interface` with `report(event)`, and `setGlobalMetadata(key, value)`, which does nothing unless overridden, as section 12 of the plan says. A Kotlin lambda or a Java lambda is a reporter. `AsyncReporter` is its asynchronous twin. From stage 3, `Reporter` is a `ReporterOf<Sync>` and `AsyncReporter` a `ReporterOf<Async>`, so an instance holds only reporters of its own mode ([0062](0062-workflow-descriptors.md)).
- **`Dispatcher`** has `add(reporter)`, `dispatch(event)` and `setGlobalMetadata(key, value)`, chapter 2, 2.2's three operations. **`DispatcherFactory`**, a `fun interface`, has `create()`. The asynchronous `AsyncDispatcherFactory.create` is a `suspend` function, since the asynchronous executor awaits it before the journey.
- **`DefaultDispatcher`**, made by **`DefaultDispatcherFactory`**, delivers to its reporters in the order they were added. A reporter that throws stops the delivery of that event or entry, and the dispatcher throws what it threw, so the executor aborts the journey with `reporter failed`. `journey_aborted` alone goes to every reporter whatever they throw, and the dispatcher then throws the first failure, which the executor ignores, as 2.3 point 4 says. An `Error` is never caught.
- **`DebugReporter`** prints each event on a line of its own, as the event's record prints, as Rust's prints an event's `Debug`. `AsyncDebugReporter` does the same for asynchronous workflows.

Every call into a reporter or a dispatcher is void, `Unit` in Kotlin and `void` in Java, as proposal 0122 point 1 asks; what one returns is never read. `DispatcherFactory.create` has a result, the dispatcher, which Kotlin's types keep from being `null`; stage 3 refuses a `null` from a Java factory, as section 11 of the plan says.

## Java's view of a record

From Java, a record's components are read with the record's accessors, such as `failed.step()`. Properties an event shares with the interface `Event` are also read through the interface's getters, such as `event.getKind()`, since Kotlin compiles an interface property to a getter. Each record has both forms, and the Java tests in `JavaEventsTest` use them.

The types refuse a source a kind does not allow, and any decider but the default for `step_retrying`, `step_rescuing` and `step_rescued`, and a compile-fail test shows each refusal.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/event/EventTest.kt`: every event class tells its kind.
- `EventKindTest.kt`: the kinds are exactly those the specification names, and each reads back from its name.
- `RescueCauseTest.kt`: how a rescue's cause prints.
- `JourneyAbortTest.kt`: each abort's reason, and its error message only when failing code caused it.
- `SourcesTest.kt`: what the sample of sources says of each.
- `NameTest.kt`, `JourneyIdTest.kt`, `AttemptTest.kt`, `ReasonTest.kt`, `HooksTest.kt`, `AbortReasonTest.kt` and `FailureCauseTest.kt`: names are never empty, equality, and how each closed set prints.
- `itinera/src/test/java/org/itineradev/itinera/event/JavaEventsTest.java`: events, names, decisions and aborts written and read in Java.
- `itinera/src/compileFailTest/kotlin/org/itineradev/itinera/event/EventTypesTest.kt`: compile-fail tests, each beside a twin that compiles in the matching `Twins` file: a contribution never comes from an input adapter, a journey-level event never from a step, and no decider but the default is given for a retry or a rescue.
- `itinera/src/test/kotlin/org/itineradev/itinera/report/DefaultDispatcherTest.kt` and `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/report/async/AsyncDefaultDispatcherTest.kt`: the order of delivery, a reporter that throws, `journey_aborted` reaching everyone, an `Error` never caught, and a dispatcher of its own for each journey.
- `ReporterTest.kt` and `AsyncReporterTest.kt`: a reporter that accepts global metadata without overriding it, and what the debug reporter prints.
- `itinera/src/test/java/org/itineradev/itinera/report/JavaReportersTest.java`: reporters and a dispatcher factory written in Java.
- `itinera-kotlin/src/test/kotlin/org/itineradev/itinera/executor/async/AsyncPartsTest.kt` and `AsyncLocalExecutorTest.kt`, with the classes of `itinera-kotlin/src/test/java/org/itineradev/itinera/executor/async/WrittenInJava.java`: an asynchronous reporter, hook or dispatcher factory written in Java is refused.

## Done when

11 is listed in both manifests once every scenario tagged `@proposal-0011` passes in both runners.
