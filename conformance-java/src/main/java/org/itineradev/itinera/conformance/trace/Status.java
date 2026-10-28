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
   * The status of {@code step}, read from the journey's {@code events}: the first event that
   * settles it, which stays final even when the journey is aborted afterwards; otherwise aborted
   * when {@code journey_aborted} names it; otherwise not executed when no attempt started; and
   * {@code null} while it was still being attempted. Rescuing waits for rescue steps to run.
   */
  public static Status of(List<Event> events, String step) {
    var settled =
        events.stream().map(event -> settled(event, step)).filter(Status::isSome).findFirst();
    if (settled.isPresent()) {
      return settled.get();
    }
    if (events.stream().anyMatch(event -> abortsAt(event, step))) {
      return ABORTED;
    }
    return attemptsOf(events, step) == 0 ? NOT_EXECUTED : null;
  }

  /** The status {@code event} settles {@code step} in, if it is one of its own that settles it. */
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
    return null;
  }

  private static boolean isSome(Status status) {
    return status != null;
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
