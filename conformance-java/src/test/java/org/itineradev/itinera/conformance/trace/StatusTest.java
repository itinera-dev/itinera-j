package org.itineradev.itinera.conformance.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
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
  void aStepBeingRescuedIsRescuingAndAbortedWhenTheJourneyIsAbortedThen() {
    var events = new Events();
    var rescuing = List.of(events.attempt("charge"), events.rescuing("charge"));
    assertEquals(Status.RESCUING, Status.of(rescuing, "charge"));
    var aborted = new ArrayList<>(rescuing);
    aborted.add(events.aborted("charge_rescue", "no client"));
    assertEquals(Status.ABORTED, Status.of(aborted, "charge"));
  }

  @Test
  void aRescuedStepIsRescuingUntilItsNextAttemptStartsWhichSettlesIt() {
    var events = new Events();
    var rescued =
        List.of(events.attempt("charge"), events.rescuing("charge"), events.rescued("charge"));
    var again = new ArrayList<>(rescued);
    again.add(events.attempt("charge", 2));
    var settled = new ArrayList<>(again);
    settled.add(events.succeeded("charge", 2));
    assertEquals(Status.RESCUING, Status.of(rescued, "charge"));
    assertNull(Status.of(again, "charge"));
    assertEquals(Status.SUCCEEDED, Status.of(settled, "charge"));
  }

  @Test
  void attemptsAreCountedFromTheirAttemptStarted() {
    var events = new Events();
    var stream =
        List.of(events.attempt("charge"), events.attempt("charge", 2), events.attempt("ship"));
    assertEquals(2, Status.attemptsOf(stream, "charge"));
  }
}
