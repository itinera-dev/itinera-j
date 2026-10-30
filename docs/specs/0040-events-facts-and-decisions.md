# 0040: Events record facts and decisions

Tech spec for [proposal 0040](https://github.com/itinera-dev/spec/blob/main/proposals/0040-events-facts-and-decisions.md), implemented in [#12](https://github.com/itinera-dev/itinera-j/issues/12). [0011](0011-events.md) describes the events themselves.

Status: started. Stage 1 added the types of decisions. The engine emits them once the stages that make each decision land.

## API

- **`Decision`**, in `org.itineradev.itinera.event`, is the sealed interface of the six decisions of chapter 2, 2.7: `StepRetrying`, `StepRescuing`, `StepRescued`, `StepGivenUp`, `JourneySucceeded` and `JourneyFailed`. Each carries `decidedBy`.
- **`DecidedBy`** is `DecidedBy.Default`, the executor's own rule, or a `DecidingHook`, with the policy and the step hook whose returned lifecycle decided.
- Decisions that only the executor's rule makes, `step_retrying`, `step_rescuing` and `step_rescued`, give `DecidedBy.Default` and take no argument for it, so no other decider can be written.
- **`StepGivenUp`** carries a `GiveUpCause`: `Failure`, `AbnormalTermination` and `RetriesExhausted`, decided by default, or `FailWorkflow`, with the reason and the hook that decided. Its `decidedBy` follows from the cause.
- **`JourneyFailed`** carries the step that failed and a `JourneyFailure`, one class for each thing a failed journey can say:
  1. `Failure`, with the reason;
  2. `AbnormalTermination`, with the message of the error;
  3. `RetriesExhaustedByFailure`, with the reason;
  4. `RetriesExhaustedByAbnormalTermination`, with the message of the error;
  5. `FailWorkflow`, with the reason and the hook that decided.

  Each gives its `cause`, a `FailureCause`, its `reason` or `errorMessage`, and `decidedBy`, so the types say which of them a failure carries, as chapter 5, 5.8 asks.

## Dependencies

None.

## Tests

- `itinera/src/test/kotlin/org/itineradev/itinera/event/DecisionTest.kt`: a step given up, and a failed journey, name their cause and who decided, for each form.
- `EventTest.kt`: every decision tells its kind; a decision by default is described as such.

## Done when

40 is listed in both manifests once every scenario tagged `@proposal-0040` passes in both runners.
