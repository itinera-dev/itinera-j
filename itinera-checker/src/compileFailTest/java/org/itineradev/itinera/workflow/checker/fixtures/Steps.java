package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.LinkedList;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.async.AsyncStepReporter;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

/** Mistakes in step classes, each in a workflow of its own. */
public final class Steps {
  private Steps() {}

  public static final class Unmarked implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "unmarked")
  @WithStep(step = Unmarked.class, name = "charge")
  public static final class UnmarkedStep implements ItineraWorkflow {}

  @Step
  public abstract static class Abstract implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "abstract")
  @WithStep(step = Abstract.class, name = "charge")
  public static final class AbstractStep implements ItineraWorkflow {}

  @Step
  public static final class TwoConstructors implements ItineraStep {
    public TwoConstructors() {}

    public TwoConstructors(@Input long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "constructors")
  @WithStep(step = TwoConstructors.class, name = "charge")
  public static final class SeveralConstructors implements ItineraWorkflow {}

  @Step
  public static final class Runless implements ItineraStep {
    public Outcome charge() {
      return succeed();
    }
  }

  @Workflow(name = "runless")
  @WithStep(step = Runless.class, name = "charge")
  public static final class NoRun implements ItineraWorkflow {}

  @Step
  public static final class TwoRuns implements ItineraStep {
    public Outcome run() {
      return succeed();
    }

    public Outcome run(Contributor contributor) {
      return succeed();
    }
  }

  @Workflow(name = "runs")
  @WithStep(step = TwoRuns.class, name = "charge")
  public static final class SeveralRuns implements ItineraWorkflow {}

  @Step
  public static final class Talking implements ItineraStep {
    public String run() {
      return "done";
    }
  }

  @Workflow(name = "talking")
  @WithStep(step = Talking.class, name = "charge")
  public static final class RunReturnsString implements ItineraWorkflow {}

  @Step
  public final class Inner implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "inner")
  @WithStep(step = Inner.class, name = "charge")
  public static final class InnerStep implements ItineraWorkflow {}

  @Step
  public static final class UnmarkedInput implements ItineraStep {
    public UnmarkedInput(long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "unmarked input")
  @WithStep(step = UnmarkedInput.class, name = "charge")
  public static final class InputNotMarked implements ItineraWorkflow {}

  @Step
  public static final class Linked implements ItineraStep {
    public Linked(@Input LinkedList<String> lines, @Input("totals") long[] totals) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "linked")
  @WithStep(step = Linked.class, name = "charge")
  public static final class InputsNoValueIs implements ItineraWorkflow {}

  @Step
  public static final class Requesting implements ItineraStep {
    public Outcome run(
        @Input long amount,
        String note,
        AsyncStepReporter reporter,
        @Service Contributor payments) {
      return succeed();
    }
  }

  @Workflow(name = "requesting")
  @WithStep(step = Requesting.class, name = "charge")
  public static final class RunRequestsWrongly implements ItineraWorkflow {}

  @Step
  public static final class BoundRun implements ItineraStep {
    public <C extends Contributor> Outcome run(C contributor) {
      return succeed();
    }
  }

  @WithStep(step = BoundRun.class, name = "bound")
  public static final class BoundContributor implements ItineraWorkflow {}
}
