package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** What a scenario scripts for one step, a rescue step included, filled in as it is written. */
public final class StepScript {
  private final List<Input> inputs = new ArrayList<>();
  private final List<RequestedService> services = new ArrayList<>();
  private final List<RescuedStepRequest> ofRescuedStep = new ArrayList<>();
  private final List<StepAction> actions = new ArrayList<>();
  private final Map<String, Object> inputChanges = new LinkedHashMap<>();
  private List<ScriptedAttempt> attempts;
  private Integer retries;
  private boolean retryOnAbnormalTermination = true;
  private String buildFailure;
  private boolean ignoresEmitFailures;
  private boolean hasRescueStep;
  private boolean isRescueStep;
  private boolean isAsynchronous;
  private List<String> policies;
  private boolean keepsHandles;
  private boolean builtAsNull;
  private boolean returnsNoOutcome;

  /** The inputs it requests, in the order written. */
  public List<Input> inputs() {
    return inputs;
  }

  /** The services it requests, in the order written. */
  public List<RequestedService> services() {
    return services;
  }

  /**
   * What a rescue step requests of the step it rescues: its data, its failure reason or its error,
   * in the order written.
   */
  public List<RescuedStepRequest> ofRescuedStep() {
    return ofRescuedStep;
  }

  /** What the step does on every attempt before it ends, in the order written. */
  public List<StepAction> actions() {
    return actions;
  }

  /** Whether the step catches what its emit calls throw and carries on. */
  public boolean ignoresEmitFailures() {
    return ignoresEmitFailures;
  }

  /** What the step changes each input it received to, in place, by key. */
  public Map<String, Object> inputChanges() {
    return inputChanges;
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

  /** The message the step's build fails with, or {@code null} until stated. */
  public String buildFailure() {
    return buildFailure;
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

  /** The step policies attached to the step, in order, or {@code null} until stated. */
  public List<String> policies() {
    return policies;
  }

  /** Whether the step keeps its contributor and step reporter after its attempt ends. */
  public boolean keepsHandles() {
    return keepsHandles;
  }

  /** Whether the step's factory returns {@code null}, not a step. */
  public boolean builtAsNull() {
    return builtAsNull;
  }

  /** Whether the step's run returns {@code null}, not an outcome. */
  public boolean returnsNoOutcome() {
    return returnsNoOutcome;
  }

  /** Says the step's factory returns {@code null}, not a step. */
  public void buildAsNull() {
    builtAsNull = true;
  }

  /** Says the step's run returns {@code null}, not an outcome. */
  public void returnNoOutcome() {
    returnsNoOutcome = true;
  }

  /** Attaches step policies to the step {@code name}, once. */
  public void attach(String name, List<String> attached) {
    if (policies != null) {
      throw new ModelException("the policies of \"" + name + "\" are stated twice");
    }
    policies = List.copyOf(attached);
  }

  /** Says the step keeps its contributor and step reporter after its attempt ends. */
  public void keepHandles() {
    keepsHandles = true;
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

  /** Says that building the step fails with {@code message}, once. */
  public void cannotBeBuilt(String name, String message) {
    if (buildFailure != null) {
      throw new ModelException("how \"" + name + "\" fails to build is stated twice");
    }
    buildFailure = message;
  }

  /** Turns {@code retry on abnormal termination} off. */
  public void doesNotRetryAbnormalTerminations() {
    retryOnAbnormalTermination = false;
  }

  /** Says the step catches what its emit calls throw and carries on. */
  public void ignoreEmitFailures() {
    ignoresEmitFailures = true;
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
