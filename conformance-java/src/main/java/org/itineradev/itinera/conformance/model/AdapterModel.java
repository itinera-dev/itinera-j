package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An input adapter the workflow declares, attached to its steps, each as often as named, with what
 * it requests in the order written and what it does for each input it is scripted for. For any
 * other input it leaves the input to the data bag.
 */
public final class AdapterModel {
  private final String name;
  private final List<String> steps;
  private final List<AdapterRequest> requests = new ArrayList<>();
  private final Map<Called, AdapterAnswer> answers = new HashMap<>();
  private String keepsAccessOf;
  private boolean asynchronous;

  /** The input an adapter is called for, for one step or for any. */
  private record Called(String step, String input) {}

  AdapterModel(String name, List<String> steps) {
    this.name = name;
    this.steps = List.copyOf(steps);
  }

  /** Its name. */
  public String name() {
    return name;
  }

  /** The steps it is attached to, each as often as named. */
  public List<String> steps() {
    return steps;
  }

  /** What it requests, in the order written. */
  public List<AdapterRequest> requests() {
    return requests;
  }

  /**
   * Says what the adapter does for {@code input}, when called for {@code step}, or for any step
   * when {@code step} is {@code null}.
   */
  public void answers(String input, AdapterAnswer answer, String step) {
    if (answers.putIfAbsent(new Called(step, input), answer) != null) {
      throw new ModelException(
          "what the adapter \"" + name + "\" does for \"" + input + "\" is stated twice");
    }
  }

  /** What the adapter does for {@code input} when called for {@code step}. */
  public AdapterAnswer answerFor(String step, String input) {
    var forStep = answers.get(new Called(step, input));
    if (forStep != null) {
      return forStep;
    }
    return answers.getOrDefault(new Called(null, input), new AdapterAnswer.Nothing());
  }

  /** The step whose call's access to the data bag the adapter keeps, or {@code null}. */
  public String keepsAccessOf() {
    return keepsAccessOf;
  }

  /** Says the adapter keeps the access to the data bag of its calls for {@code step}, once. */
  public void keepsAccessOf(String step) {
    if (keepsAccessOf != null) {
      throw new ModelException("which access the adapter \"" + name + "\" keeps is stated twice");
    }
    keepsAccessOf = step;
  }

  /** Whether the adapter runs asynchronously. */
  public boolean isAsynchronous() {
    return asynchronous;
  }

  /** Says the adapter runs asynchronously. */
  public void markAsynchronous() {
    asynchronous = true;
  }
}
