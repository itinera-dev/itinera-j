package org.itineradev.itinera.conformance.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a scenario of test support declares: whether the instance has the recording reporter of test
 * support, the fakes put in place of service providers, by the provider's name, and which recorders
 * lent outside a journey fail.
 */
public final class TestSupport {
  private boolean recordingReporter;
  private final Map<String, FakeModel> fakes = new LinkedHashMap<>();
  private boolean stepRecorderFails;
  private boolean hookRecorderFails;
  private List<ScriptRow> script;
  private boolean ensuresAllScripted;

  /** Whether the instance has the recording reporter of test support. */
  public boolean recordingReporter() {
    return recordingReporter;
  }

  /** The fakes, by the name of the provider each replaces, in the order written. */
  public Map<String, FakeModel> fakes() {
    return fakes;
  }

  /** Whether the step event reporter is failing. */
  public boolean stepRecorderFails() {
    return stepRecorderFails;
  }

  /** Whether the hook event reporter is failing. */
  public boolean hookRecorderFails() {
    return hookRecorderFails;
  }

  /** The rows of the script, in the order written, or {@code null} until it is written. */
  public List<ScriptRow> script() {
    return script;
  }

  /** Whether the script ensures every step is scripted. */
  public boolean ensuresAllScripted() {
    return ensuresAllScripted;
  }

  /** Writes the script, once. */
  public void script(List<ScriptRow> rows) {
    if (script != null) {
      throw new ModelException("the script is written twice");
    }
    script = List.copyOf(rows);
  }

  /** Makes the script ensure every step is scripted. */
  public void ensureAllScripted() {
    ensuresAllScripted = true;
  }

  /** Whether the instance is created through test support. */
  public boolean createsTheInstance() {
    return recordingReporter || !fakes.isEmpty();
  }

  /** Gives the instance the recording reporter of test support. */
  public void record() {
    recordingReporter = true;
  }

  /** Puts {@code fake} in place of the provider named {@code provider}, once. */
  public void fake(String provider, FakeModel fake) {
    if (fakes.containsKey(provider)) {
      throw new ModelException("the provider \"" + provider + "\" is faked twice");
    }
    fakes.put(provider, fake);
  }

  /** Makes the step event reporter fail. */
  public void failStepRecorder() {
    stepRecorderFails = true;
  }

  /** Makes the hook event reporter fail. */
  public void failHookRecorder() {
    hookRecorderFails = true;
  }
}
