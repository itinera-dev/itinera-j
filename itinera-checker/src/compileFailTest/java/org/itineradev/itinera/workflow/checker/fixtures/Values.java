package org.itineradev.itinera.workflow.checker.fixtures;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.itinera.fixtures.Mood;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;

/** Inputs of types that no value can be, and of types every value can be. */
public final class Values {
  private Values() {}

  public enum Colour {
    RED,
    BLUE;

    public int shade;
  }

  public static final class Plain {
    public String name;
  }

  public static final class Frozen {
    public final String name;

    public Frozen(String name) {
      this.name = name;
    }
  }

  public record Line(String product, long quantity, List<String> parts) {}

  public abstract static class Shape {}

  @Step
  public static final class Refused<T> implements ItineraStep {
    public Refused(
        @Input("colour") Colour colour,
        @Input("plain") Plain plain,
        @Input("by number") Map<Integer, String> byNumber,
        @Input("nested") Map<String, Map<Long, String>> nested,
        @Input("generic") T generic,
        @Input("lines") Set<Line[]> lines,
        @Input("shape") Shape shape,
        @Input("thread") Thread thread,
        @Input("frozen") Frozen frozen,
        @Input("weights") Map<Line, Double> weights,
        @Input("mood") Mood mood) {}

    public Outcome run() {
      return succeed();
    }
  }

  @WithStep(step = Refused.class, name = "refused")
  public static final class Unvalued implements ItineraWorkflow {}

  @Step
  public static final class Accepted implements ItineraStep {
    public Accepted(
        @Input("lines") List<Line> lines,
        @Input("names") Map<String, List<String>> names,
        @Input("anything") Object anything,
        @Input("number") Number number,
        @Input("unit") ChronoUnit unit) {}

    public Outcome run() {
      return succeed();
    }
  }

  @WithStep(step = Accepted.class, name = "accepted")
  public static final class Valued implements ItineraWorkflow {}
}
