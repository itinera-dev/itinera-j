# 0064: Event data and reason details are values, serialized only by reporters

Tech spec for [proposal 0064](https://github.com/itinera-dev/spec/blob/main/proposals/0064-event-data-values.md), implemented in [#25](https://github.com/itinera-dev/itinera-j/issues/25). [0056](0056-data-bag-values.md) says what a value is, and [0011](0011-events.md) describes the events.

Status: started. Stage 1 typed event data and reason details as values. Stage 5 captures the data a service provider supplies with an instance and the details of steps' reasons, then what steps emit, and stages 7 and 8 what hooks and service providers emit.

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

Stage 5 adds the tests of capture at emission, since steps emit from there.

## Done when

64 is listed in both manifests once every scenario tagged `@proposal-0064` passes in both runners.
