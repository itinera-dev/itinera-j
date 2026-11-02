package org.itineradev.itinera.conformance.trace;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.event.AttemptStarted;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.StepGivenUp;
import org.itineradev.itinera.event.StepRescuing;
import org.itineradev.itinera.event.StepSkipped;
import org.itineradev.itinera.event.StepSucceeded;

/** The status a step ends a journey in, as the cases name it. */
public enum Status {
  SUCCEEDED("succeeded"),
  FAILED("failed"),
  SKIPPED("skipped"),
  NOT_EXECUTED("not executed"),
  RESCUING("rescuing"),
  ABORTED("aborted");

  private static final Map<String, Status> BY_WRITTEN =
      Arrays.stream(values()).collect(Collectors.toMap(Status::written, Function.identity()));

  private final String written;

  Status(String written) {
    this.written = written;
  }

  /** How the cases write it. */
  public String written() {
    return written;
  }

  /** The status a case names. */
  public static Status named(String written) {
    var status = BY_WRITTEN.get(written);
    if (status == null) {
      throw new ModelException("\"" + written + "\" is not a step status");
    }
    return status;
  }

  /**
   * The status of {@code step}, read from the journey's {@code events} in order: the first event
   * that settles it stays final even when the journey is aborted afterwards; {@code step_rescuing}
   * makes it rescuing until its next {@code attempt_started}; {@code journey_aborted} aborts it
   * when it names it or while it is rescuing. Otherwise it is not executed when no attempt started,
   * and {@code null} while it was still being attempted.
   */
  public static Status of(List<Event> events, String step) {
    var status =
        events.stream()
            .<Status>reduce(
                null, (sofar, event) -> next(sofar, event, step), (earlier, later) -> later);
    if (status != null) {
      return status;
    }
    return attemptsOf(events, step) == 0 ? NOT_EXECUTED : null;
  }

  /**
   * The status of {@code step} once {@code event} follows its {@code status}, which a final status
   * keeps.
   */
  private static Status next(Status status, Event event, String step) {
    return status == null || status == RESCUING ? after(status, event, step) : status;
  }

  private static Status after(Status status, Event event, String step) {
    if (event instanceof JourneyAborted && (status == RESCUING || abortsAt(event, step))) {
      return ABORTED;
    }
    if (status == RESCUING && startsAnAttempt(event, step)) {
      return null;
    }
    var settled = settled(event, step);
    return settled == null ? status : settled;
  }

  /** The status {@code event} puts {@code step} in, if it is one of its own that does. */
  private static Status settled(Event event, String step) {
    if (event instanceof StepSucceeded succeeded && succeeded.step().toString().equals(step)) {
      return SUCCEEDED;
    }
    if (event instanceof StepSkipped skipped && skipped.step().toString().equals(step)) {
      return SKIPPED;
    }
    if (event instanceof StepGivenUp givenUp && givenUp.step().toString().equals(step)) {
      return FAILED;
    }
    if (event instanceof StepRescuing rescuing && rescuing.step().toString().equals(step)) {
      return RESCUING;
    }
    return null;
  }

  /** Whether {@code event} is a {@code journey_aborted} naming {@code step}. */
  public static boolean abortsAt(Event event, String step) {
    return event instanceof JourneyAborted aborted
        && aborted.abort().getStep() != null
        && aborted.abort().getStep().toString().equals(step);
  }

  /** How many attempts of {@code step} started. */
  public static int attemptsOf(List<Event> events, String step) {
    return (int) events.stream().filter(event -> startsAnAttempt(event, step)).count();
  }

  private static boolean startsAnAttempt(Event event, String step) {
    return event instanceof AttemptStarted started && started.step().toString().equals(step);
  }
}
