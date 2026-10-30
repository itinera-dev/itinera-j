# 0011: Events

Tech spec for [proposal 0011](https://github.com/itinera-dev/spec/blob/main/proposals/0011-events.md), implemented in [#7](https://github.com/itinera-dev/itinera-j/issues/7). The [tier 1 plan](../tier-1-plan.md), section 12, holds what crosses proposals; [0040](0040-events-facts-and-decisions.md) covers decisions and [0064](0064-event-data-values.md) the data events carry.

Status: started. Stage 1 added the events and what they carry. Reporters, dispatchers and their factories follow in the next layer of stage 1. The engine emits events from stage 3.

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

## Java's view of a record

From Java, a record's components are read with the record's accessors, such as `failed.step()`. Properties an event shares with the interface `Event` are also read through the interface's getters, such as `event.getKind()`, since Kotlin compiles an interface property to a getter. Each record has both forms, and the Java tests in `JavaEventsTest` use them.

The types refuse a source a kind does not allow, and any decider but the default for `step_retrying`, `step_rescuing` and `step_rescued`. These refusals get compile-fail tests in stage 3, when the build gains kctfork for the refusals of the builders and executors (section 16 of the plan).

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

## Done when

11 is listed in both manifests once every scenario tagged `@proposal-0011` passes in both runners.
