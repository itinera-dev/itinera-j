package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.step.SyncStepFactory;
import org.itineradev.itinera.step.SyncStepRun;

/** Every step the scenario's workflow built, by step name, in the order built, and how many ran. */
public final class Witness {
  private final Map<String, List<Object>> built = new LinkedHashMap<>();
  private int ran;

  /** How many built steps ran. */
  public int ran() {
    return ran;
  }

  /** The steps built under {@code step}, in the order they were built. */
  public List<Object> built(String step) {
    return List.copyOf(built.getOrDefault(step, List.of()));
  }

  /**
   * A factory building with {@code factory}, each step kept under {@code step} as it is built and
   * counted as it runs.
   */
  SyncStepFactory watching(String step, SyncStepFactory factory) {
    return () -> watched(step, factory.build());
  }

  private SyncStepRun watched(String step, SyncStepRun run) {
    built.computeIfAbsent(step, unbuilt -> new ArrayList<>()).add(run);
    return () -> {
      ran += 1;
      return run.run();
    };
  }
}
