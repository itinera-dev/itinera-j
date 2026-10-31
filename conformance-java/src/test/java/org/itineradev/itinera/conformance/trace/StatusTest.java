package org.itineradev.itinera.conformance.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

final class StatusTest {
  @Test
  void aStepSettledByItsOwnEventKeepsThatStatus() {
    var events = new Events();
    var stream =
        List.of(events.attempt("charge"), events.succeeded("charge"), events.aborted("ship", "x"));
    assertEquals(Status.SUCCEEDED, Status.of(stream, "charge"));
  }

  @Test
  void aStepGivenUpFailed() {
    var events = new Events();
    assertEquals(
        Status.FAILED,
        Status.of(List.of(events.attempt("charge"), events.givenUp("charge")), "charge"));
  }

  @Test
  void aStepNamedByJourneyAbortedBeforeItSettledWasAborted() {
    assertEquals(
        Status.ABORTED, Status.of(List.of(new Events().aborted("charge", "no gateway")), "charge"));
  }

  @Test
  void aStepNoAttemptOfWhichStartedWasNotExecuted() {
    assertEquals(Status.NOT_EXECUTED, Status.of(List.of(new Events().started()), "charge"));
  }

  @Test
  void aStepStillBeingAttemptedHasNoStatusYet() {
    assertNull(Status.of(List.of(new Events().attempt("charge")), "charge"));
  }

  @Test
  void attemptsAreCountedFromTheirAttemptStarted() {
    var events = new Events();
    var stream =
        List.of(events.attempt("charge"), events.attempt("charge", 2), events.attempt("ship"));
    assertEquals(2, Status.attemptsOf(stream, "charge"));
  }
}
