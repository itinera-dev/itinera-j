package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** What a scenario scripts for one step, a rescue step included, filled in as it is written. */
public final class StepScript {
  private final List<Input> inputs = new ArrayList<>();
  private final List<Map.Entry<String, Object>> contributions = new ArrayList<>();
  private List<ScriptedAttempt> attempts;
  private Integer retries;
  private boolean retryOnAbnormalTermination = true;
  private boolean hasRescueStep;
  private boolean isRescueStep;
  private boolean isAsynchronous;

  /** The inputs it requests, in the order written. */
  public List<Input> inputs() {
    return inputs;
  }

  /** What it contributes, in the order written. */
  public List<Map.Entry<String, Object>> contributions() {
    return contributions;
  }

  /** Each attempt in order; attempts after the last repeat it. {@code null} until stated. */
  public List<ScriptedAttempt> attempts() {
    return attempts;
  }

  /** Its retry budget, or {@code null} until stated. */
  public Integer retries() {
    return retries;
  }

  /** Whether an abnormal termination is retried while the retry budget lasts. */
  public boolean retryOnAbnormalTermination() {
    return retryOnAbnormalTermination;
  }

  /** Whether the step has a rescue step. */
  public boolean hasRescueStep() {
    return hasRescueStep;
  }

  /** Whether this is the script of a rescue step. */
  public boolean isRescueStep() {
    return isRescueStep;
  }

  /** Whether the step is asynchronous. */
  public boolean isAsynchronous() {
    return isAsynchronous;
  }

  /** Says how each attempt ends, once. */
  public void attempts(String name, List<ScriptedAttempt> scripted) {
    if (attempts != null) {
      throw new ModelException("how the attempts of \"" + name + "\" end is stated twice");
    }
    if (!IntStream.range(0, scripted.size()).allMatch(index -> numbered(scripted, index))) {
      throw new ModelException(
          "the attempts of \"" + name + "\" are not numbered 1, 2, 3 and so on");
    }
    attempts = List.copyOf(scripted);
  }

  /** Gives the step its retry budget, once. */
  public void allows(String name, int budget) {
    if (retries != null) {
      throw new ModelException("the retry budget of \"" + name + "\" is stated twice");
    }
    if (budget < 0) {
      throw new ModelException("\"" + name + "\" cannot allow " + budget + " retries");
    }
    retries = budget;
  }

  /** Turns {@code retry on abnormal termination} off. */
  public void doesNotRetryAbnormalTerminations() {
    retryOnAbnormalTermination = false;
  }

  /** Says the step has a rescue step. */
  void giveARescueStep() {
    hasRescueStep = true;
  }

  /** Says this is the script of a rescue step. */
  void markAsRescueStep() {
    isRescueStep = true;
  }

  /** Says the step is asynchronous. */
  public void markAsynchronous() {
    isAsynchronous = true;
  }

  private static boolean numbered(List<ScriptedAttempt> scripted, int index) {
    return scripted.get(index).number() == index + 1;
  }
}
