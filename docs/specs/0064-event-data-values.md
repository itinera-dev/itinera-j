# 0064: Event data and reason details are values, serialized only by reporters

Tech spec for [proposal 0064](https://github.com/itinera-dev/spec/blob/main/proposals/0064-event-data-values.md), implemented in [#25](https://github.com/itinera-dev/itinera-j/issues/25). [0056](0056-data-bag-values.md) says what a value is, and [0011](0011-events.md) describes the events.

Status: done. Stage 1 typed event data and reason details as values. Stage 5 captures the data a service provider supplies with an instance, the details of steps' reasons and the data steps emit. Stage 6 lists 64 in both manifests. Stages 7 and 8 will capture what hooks and service providers emit through the same `Values.capture`, as [0011](0011-events.md) and [0095](0095-services-provided-to-steps.md) say.

## API

- The data of `step_info`, `step_warning`, `step_error`, `journey_info`, `journey_warning`, `journey_error` and `service_provided`, and the details of a `Reason`, are typed `Any?`. The engine captures each with `Values.capture` when it is given, so the event holds itinera's own copy, and changing the original afterwards changes nothing. A step's reason is given to `Outcome.failure` or `Outcome.skipped`, or to the methods of `ItineraStep` that call them, which capture its details then; the engine captures them again once the attempt returns the outcome, since the step can still reach the outcome's copy, and the events carry that second copy. Details that are not a value are kept as given, and the engine, capturing them again, aborts the journey with `not a value`, naming the step, with no outcome fact.
- Every reporter receives the event's own captured copy, as section 2 of the plan says: reporters are not receivers in the sense of chapter 4, 4.7. The documentation of `Event` says that a reporter must not change it.
- Nothing is serialized. A reporter that writes JSON serializes the data with the library it chooses.
- An event's `data` is `null` when the author gave none, and also when it gave `null`. No rule tells the two apart, and a reporter writes both the same way.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/LendingTest.kt`: "data a provider gives that is not a value aborts the journey".
- `itinera/src/test/kotlin/org/itineradev/itinera/step/OutcomeTest.kt`: "a failure's details are captured when it is made", "a skip's details are captured when it is made" and "details that are not a value are kept as given, for the engine to refuse".
- `itinera/src/test/kotlin/org/itineradev/itinera/engine/AttemptingTest.kt`: "a reason whose details are not a value aborts the journey with no outcome fact".

- `itinera/src/test/kotlin/org/itineradev/itinera/engine/StepEmitsTest.kt`: "an event carries a copy of its data made when it is emitted" and "data that is not a value aborts the journey, and the event is not delivered".

## Conformance

Of the two scenarios tagged `@proposal-0064`, "A failure whose details are not a value aborts the journey" runs once 64 is listed, and passes in both runners and every column. "Data that is not a value aborts the journey, and the event is not delivered" is also tagged `@proposal-0011`, so it runs from stage 7, which lists 11, and passes in both runners and every column; `StepEmitsTest` checks its rule too.

## Done when

64 is listed in both manifests by the pull request of stage 6 that lists 61 and 63, which says `Closes #25`.
