package org.itineradev.itinera.conformance.trace;

import java.time.Instant;
import java.util.Set;
import org.itineradev.itinera.event.AttemptStarted;
import org.itineradev.itinera.event.ByStep;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.GiveUpCause;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.JourneyStarted;
import org.itineradev.itinera.event.RescueCause;
import org.itineradev.itinera.event.StepAbnormalTermination;
import org.itineradev.itinera.event.StepCouldNotBeBuilt;
import org.itineradev.itinera.event.StepGivenUp;
import org.itineradev.itinera.event.StepInfo;
import org.itineradev.itinera.event.StepRescued;
import org.itineradev.itinera.event.StepRescuing;
import org.itineradev.itinera.event.StepSucceeded;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.WorkflowName;

/** Events of a journey of the workflow {@code orders}, numbered as they are made. */
public final class Events {
  private static final Instant AT = Instant.parse("2026-10-28T02:00:00Z");
  private static final JourneyId JOURNEY = JourneyId.of("order-1");
  private static final WorkflowName ORDERS = WorkflowName.of("orders");

  private long sequence;

  private long next() {
    sequence += 1;
    return sequence;
  }

  public Event started() {
    return new JourneyStarted(next(), AT, JOURNEY, ORDERS, Set.of("amount"));
  }

  public Event attempt(String step) {
    return attempt(step, 1);
  }

  public Event attempt(String step, int number) {
    return new AttemptStarted(next(), AT, JOURNEY, ORDERS, StepName.of(step), Attempt.of(number));
  }

  public Event succeeded(String step) {
    return succeeded(step, 1);
  }

  public Event succeeded(String step, int number) {
    return new StepSucceeded(next(), AT, JOURNEY, ORDERS, StepName.of(step), Attempt.of(number));
  }

  public Event terminated(String step, String message) {
    return new StepAbnormalTermination(
        next(), AT, JOURNEY, ORDERS, StepName.of(step), Attempt.of(1), message);
  }

  public Event givenUp(String step) {
    return new StepGivenUp(
        next(),
        AT,
        JOURNEY,
        ORDERS,
        StepName.of(step),
        Attempt.of(1),
        GiveUpCause.AbnormalTermination.INSTANCE);
  }

  public Event aborted(String step, String message) {
    return new JourneyAborted(
        next(),
        AT,
        JOURNEY,
        ORDERS,
        new StepCouldNotBeBuilt(StepName.of(step), ByStep.INSTANCE, message));
  }

  public Event rescuing(String step) {
    return new StepRescuing(
        next(),
        AT,
        JOURNEY,
        ORDERS,
        StepName.of(step),
        Attempt.of(1),
        RescueCause.FAILURE,
        StepName.of(step + "_rescue"));
  }

  public Event rescued(String step) {
    return new StepRescued(
        next(),
        AT,
        JOURNEY,
        ORDERS,
        StepName.of(step),
        Attempt.of(1),
        StepName.of(step + "_rescue"));
  }

  public Event info(String step, String message, Object data) {
    return new StepInfo(
        next(), AT, JOURNEY, ORDERS, StepName.of(step), Attempt.of(1), message, data);
  }
}
