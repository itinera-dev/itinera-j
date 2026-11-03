package org.itineradev.itinera.workflow.checker.fixtures;

import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.report.Reporter;
import org.itineradev.itinera.report.ReporterOf;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.Async;
import org.itineradev.itinera.workflow.IdGenerator;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

/** Mistakes in workflow classes. */
public final class Workflows {
  private Workflows() {}

  @Step
  public static final class Pack implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class Ship implements ItineraStep {
    public Outcome run() {
      return Outcome.success();
    }
  }

  @Workflow(name = "shipping")
  @WithStep(step = Pack.class, name = "pack")
  @WithStep(step = Ship.class, name = "ship", retryBudget = 2, noRetryOnAbnormalTermination = true)
  public static final class Shipping implements ItineraWorkflow {
    private final String warehouse;

    Shipping(String warehouse) {
      this.warehouse = warehouse;
    }

    @IdGenerator
    public String parcelId(DataBag data) {
      return warehouse + "-" + data.read("parcel", TypeToken.of(String.class)).orNull();
    }
  }

  @WithStep(step = Pack.class, name = "pack")
  public static final class Unnamed implements ItineraWorkflow {}

  public static final class Printer implements Reporter {
    @Override
    public void report(Event event) {}
  }

  @Workflow(name = "printed", reporters = Printer.class)
  @WithStep(step = Pack.class, name = "pack")
  public static final class Printed implements ItineraWorkflow {}

  @Step
  public static final class TwoRunsStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }

    public Outcome run(String parcel) {
      return succeed();
    }
  }

  @Step
  public static final class StringStep implements ItineraStep {
    public String run() {
      return "done";
    }
  }

  @Step
  public static final class RequestingStep implements ItineraStep {
    public Outcome run(String parcel) {
      return succeed();
    }
  }

  @Step
  public static final class ConstructedStep implements ItineraStep {
    ConstructedStep(String parcel) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public abstract static class Abstract implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class Nothing implements ItineraStep {}

  @WithStep(step = Nothing.class, name = "pack")
  public static final class NoRun implements ItineraWorkflow {}

  @WithStep(step = TwoRunsStep.class, name = "pack")
  public static final class TwoRuns implements ItineraWorkflow {}

  @WithStep(step = StringStep.class, name = "pack")
  public static final class Returning implements ItineraWorkflow {}

  @WithStep(step = RequestingStep.class, name = "pack")
  public static final class Requesting implements ItineraWorkflow {}

  @WithStep(step = ConstructedStep.class, name = "pack")
  public static final class Constructed implements ItineraWorkflow {}

  @WithStep(step = Abstract.class, name = "pack")
  public static final class AbstractStep implements ItineraWorkflow {}

  @WithStep(step = Pack.class, name = "")
  @WithStep(step = Ship.class, name = "ship", retryBudget = -1)
  public static final class BadSteps implements ItineraWorkflow {}

  @Workflow(name = "")
  public static final class EmptyName implements ItineraWorkflow {}

  @WithStep(step = Pack.class, name = "pack")
  @WithStep(step = Ship.class, name = "pack")
  public static final class Twice implements ItineraWorkflow {}

  public static final class TwoGenerators implements ItineraWorkflow {
    @IdGenerator
    public String first() {
      return "a";
    }

    @IdGenerator
    public String second() {
      return "b";
    }
  }

  public static final class ObjectIds implements ItineraWorkflow {
    @IdGenerator
    public Object id() {
      return "a";
    }
  }

  public static final class NumberedIds implements ItineraWorkflow {
    @IdGenerator
    public String id(long number) {
      return "a" + number;
    }
  }

  public abstract static class AbstractReporter implements Reporter {}

  public static final class ConstructedReporter implements Reporter {
    ConstructedReporter(String prefix) {}

    @Override
    public void report(Event event) {}
  }

  public static final class AsyncPrinter implements ReporterOf<Async> {}

  @Workflow(name = "reporting", reporters = AbstractReporter.class)
  public static final class AbstractReporting implements ItineraWorkflow {}

  @Workflow(name = "reporting", reporters = ConstructedReporter.class)
  public static final class ConstructedReporting implements ItineraWorkflow {}

  @Workflow(name = "reporting", reporters = AsyncPrinter.class)
  public static final class AsyncReporting implements ItineraWorkflow {}

  public final class InnerReporter implements Reporter {
    @Override
    public void report(Event event) {}
  }

  @Workflow(name = "inner reporting", reporters = InnerReporter.class)
  @WithStep(step = Pack.class, name = "pack")
  public static final class InnerReporting implements ItineraWorkflow {}

  @WithStep(step = Pack.class, name = "pack")
  public static final class BoundIds implements ItineraWorkflow {
    @IdGenerator
    public <D extends DataBag> String id(D data) {
      return "order";
    }
  }
}
