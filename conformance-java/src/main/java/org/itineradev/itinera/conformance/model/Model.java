package org.itineradev.itinera.conformance.model;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a scenario declares before it acts, as plain data, filled by its Given sentences. A
 * later stage builds the workflow, its instance and the executor from it, through itinera's public
 * API. A sentence that does not fit what is already declared is an error in the case.
 */
public final class Model {
  private WorkflowModel declared;
  private boolean startsEmpty;
  private final Map<String, Object> initialData = new LinkedHashMap<>();
  private final List<Map.Entry<String, Object>> initialGlobalMetadata = new ArrayList<>();
  private final Map<String, ReporterFailure> reporterFailures = new LinkedHashMap<>();
  private Dispatching dispatching = new Dispatching.Unstated();
  private boolean onlySynchronous;

  /** The initial data of the workflow instance, in the order written. */
  public Map<String, Object> initialData() {
    return initialData;
  }

  /** The initial global metadata of the workflow instance, each entry in the order written. */
  public List<Map.Entry<String, Object>> initialGlobalMetadata() {
    return initialGlobalMetadata;
  }

  /** How each scripted reporter fails, by name; one not here never fails. */
  public Map<String, ReporterFailure> reporterFailures() {
    return reporterFailures;
  }

  /** What the executor is given to dispatch events. */
  public Dispatching dispatching() {
    return dispatching;
  }

  /** Whether the executor accepts only synchronous steps, hooks, reporters and providers. */
  public boolean onlySynchronous() {
    return onlySynchronous;
  }

  /** The workflow the scenario declared. */
  public WorkflowModel workflow() {
    if (declared == null) {
      throw new ModelException("no workflow is declared yet");
    }
    return declared;
  }

  /** Declares the workflow, once, with its steps in declaration order. */
  public void declare(String name, List<String> steps) {
    if (declared != null) {
      throw new ModelException("a workflow is already declared");
    }
    declared = new WorkflowModel(name, steps);
  }

  /** The script of a step, or of a rescue step, of the workflow. */
  public StepScript step(String name) {
    var script = workflow().scripts().get(name);
    if (script == null) {
      throw new ModelException("the workflow has no step \"" + name + "\"");
    }
    return script;
  }

  /**
   * Gives the step a rescue step, scripted under the name {@code <step>_rescue}. When a step of the
   * workflow already has that name, the two share one script, as two steps of one name do.
   */
  public void rescue(String name) {
    step(name).giveARescueStep();
    workflow()
        .scripts()
        .computeIfAbsent(name + "_rescue", unscripted -> new StepScript())
        .markAsRescueStep();
  }

  /** Says that the journey starts with no initial data. */
  public void emptyDataBag() {
    if (!initialData.isEmpty()) {
      throw new ModelException("the data bag already holds initial data");
    }
    startsEmpty = true;
  }

  /** Adds initial data, each key once. */
  public void contains(String key, Object value) {
    if (startsEmpty) {
      throw new ModelException("the data bag was said to be empty");
    }
    if (initialData.containsKey(key)) {
      throw new ModelException("the data bag holds \"" + key + "\" twice");
    }
    initialData.put(key, value);
  }

  /** Sets an entry of the initial global metadata, after those set before. */
  public void globalMetadata(String key, Object value) {
    initialGlobalMetadata.add(new SimpleImmutableEntry<>(key, value));
  }

  /** Lists the workflow's reporters, once. */
  public void listReporters(List<String> reporters) {
    if (workflow().reporters() != null) {
      throw new ModelException("the reporters are listed twice");
    }
    workflow().reporters(reporters);
  }

  /** Says where the instance's journey ID comes from, once. */
  public void generateIdsWith(IdGenerator generator) {
    if (workflow().idGenerator() != null) {
      throw new ModelException("the ID generator is stated twice");
    }
    workflow().idGenerator(generator);
  }

  /** Says how a scripted reporter fails, once. */
  public void fails(String reporter, ReporterFailure failure) {
    if (reporterFailures.containsKey(reporter)) {
      throw new ModelException("how the reporter \"" + reporter + "\" fails is stated twice");
    }
    reporterFailures.put(reporter, failure);
  }

  /** Says what the executor is given to dispatch events, once. */
  public void dispatchWith(Dispatching given) {
    if (!(dispatching instanceof Dispatching.Unstated)) {
      throw new ModelException("what the executor dispatches with is stated twice");
    }
    dispatching = given;
  }

  /** Says the executor accepts only synchronous parts. */
  public void acceptOnlySynchronous() {
    onlySynchronous = true;
  }
}
