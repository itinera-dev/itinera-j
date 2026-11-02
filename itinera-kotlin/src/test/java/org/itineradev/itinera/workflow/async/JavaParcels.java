package org.itineradev.itinera.workflow.async;

import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.WithStep;

/**
 * An asynchronous workflow written in Java, whose step can only be synchronous, since Java cannot
 * write a {@code suspend} run.
 */
@WithStep(step = JavaParcels.Pack.class, name = "pack")
public final class JavaParcels implements ItineraAsyncWorkflow {
  /** A step written in Java. */
  @Step
  public static final class Pack implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }
}
