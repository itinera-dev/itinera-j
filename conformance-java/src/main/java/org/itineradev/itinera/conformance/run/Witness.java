package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.ProviderRequest;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.SyncStepFactory;
import org.itineradev.itinera.step.SyncStepRun;

/**
 * Every step the scenario's workflow built, by step name, in the order built, how many ran, and
 * what each service provider received on each call.
 */
public final class Witness {
  private final Map<String, List<Built>> built = new LinkedHashMap<>();
  private final Map<String, List<Map<ProviderRequest, Object>>> calls = new LinkedHashMap<>();
  private int ran;

  /** How many built steps ran. */
  public int ran() {
    return ran;
  }

  /** The steps built under {@code step}, in the order they were built. */
  public List<Built> built(String step) {
    return List.copyOf(built.getOrDefault(step, List.of()));
  }

  /** What the provider {@code provider} received on each call, in order. */
  public List<Map<ProviderRequest, Object>> calls(String provider) {
    return List.copyOf(calls.getOrDefault(provider, List.of()));
  }

  /** Records a call of the provider {@code provider}, which received {@code received}. */
  void called(String provider, Map<ProviderRequest, Object> received) {
    calls.computeIfAbsent(provider, uncalled -> new ArrayList<>()).add(received);
  }

  /**
   * A factory building with {@code factory}, each step kept under {@code step} with what it
   * received as it is built, and counted as it runs, where it first acts as {@code scripted} says.
   */
  SyncStepFactory watching(String step, SyncStepFactory factory, Scripted scripted) {
    return got -> {
      var received = scripted.receivedFrom(got);
      var run = factory.build(got);
      built.computeIfAbsent(step, unbuilt -> new ArrayList<>()).add(new Built(run, received));
      return () -> ran(run, received, scripted);
    };
  }

  private Outcome ran(SyncStepRun run, Received received, Scripted scripted) {
    ran += 1;
    scripted.act(received);
    return run.run();
  }
}
