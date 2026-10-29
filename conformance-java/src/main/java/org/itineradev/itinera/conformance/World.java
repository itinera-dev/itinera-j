package org.itineradev.itinera.conformance;

import java.util.List;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.run.Admission;
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Journey;
import org.itineradev.itinera.conformance.run.LocalJourneys;
import org.itineradev.itinera.conformance.run.RanAlone;
import org.itineradev.itinera.conformance.run.Recorders;
import org.itineradev.itinera.conformance.run.Streams;
import org.itineradev.itinera.conformance.run.Tested;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.conformance.run.Witness;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.executor.testing.ScriptedRun;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.workflow.ListedStep;
import org.itineradev.itinera.workflow.Supplied;
import org.itineradev.itinera.workflow.Violations;

/**
 * What one scenario's sentences share: the model its Given sentences fill, and what its When
 * sentences did. Cucumber makes one for each scenario and gives it to every class of sentences.
 */
public final class World {
  private final Model model = new Model();
  private final Recorders recorders = new Recorders(model.reporterFailures());
  private final Witness witness = new Witness();
  private final Tested tested = new Tested(model.testing());
  private Admission admitted;
  private RanAlone ranAlone;
  private Supplied returned;
  private ScriptedRun scriptedRun;
  private List<Journey> journeys = List.of();

  /** What the scenario declares. */
  public Model model() {
    return model;
  }

  /** Every recording reporter of the scenario, by name. */
  public Recorders recorders() {
    return recorders;
  }

  /** Every step the scenario's workflow built. */
  public Witness witness() {
    return witness;
  }

  /** The test support the scenario's journeys use. */
  public Tested tested() {
    return tested;
  }

  /** What the step or hook the scenario ran alone was lent and did. */
  public RanAlone ranAlone() {
    if (ranAlone == null) {
      throw new ModelException("no step or hook ran alone");
    }
    return ranAlone;
  }

  /** Keeps what the step or hook the scenario ran alone was lent and did. */
  public void ranAlone(RanAlone ran) {
    ranAlone = ran;
  }

  /** What the input adapter the scenario called alone supplied. */
  public Supplied returned() {
    if (returned == null) {
      throw new ModelException("no input adapter was called alone");
    }
    return returned;
  }

  /** Keeps what the input adapter the scenario called alone supplied. */
  public void returned(Supplied supplied) {
    returned = supplied;
  }

  /** Every journey the scenario ran, in order. */
  public List<Journey> journeys() {
    return journeys;
  }

  /** Builds the workflow the scenario declares, once. */
  public Admission admit() {
    if (admitted == null) {
      admitted = admitted();
    }
    return admitted;
  }

  private Admission admitted() {
    try {
      return new Admission.Admitted(LocalJourneys.of(model, recorders, witness, tested));
    } catch (Violations violations) {
      return new Admission.Refused(violations);
    }
  }

  /** Runs one journey of the workflow with the scenario's script, once it is admitted. */
  public void runScripted() {
    if (admit() instanceof Admission.Admitted admission) {
      var scripted = admission.journeys().runScripted();
      journeys = List.of(scripted.journey());
      scriptedRun = scripted.run();
    }
  }

  /** The record of the scripted run. */
  public ScriptedRun scriptedRun() {
    if (scriptedRun == null) {
      throw new Unmet("expected a scripted run; " + journey().ended());
    }
    return scriptedRun;
  }

  /** Runs {@code times} journeys of the workflow on one executor, once it is admitted. */
  public void run(int times) {
    if (admit() instanceof Admission.Admitted admission) {
      journeys = admission.journeys().run(times);
    }
  }

  /**
   * The listing of the workflow, once admitted.
   *
   * @throws Unmet when the builder refused the workflow
   */
  public List<ListedStep> listing() {
    var admission = admit();
    if (admission instanceof Admission.Refused refused) {
      throw new Unmet(
          "expected a listing; the workflow was refused: " + refused.violations().getMessage());
    }
    return ((Admission.Admitted) admission).journeys().listing();
  }

  /** What building the workflow gave, once a When sentence built it. */
  public Admission admission() {
    if (admitted == null) {
      throw new ModelException("the workflow was neither admitted nor run");
    }
    return admitted;
  }

  /**
   * The scenario's journey, the last when it ran several.
   *
   * @throws Unmet when the builder refused the workflow, so no journey ran
   */
  public Journey journey() {
    if (!journeys.isEmpty()) {
      return journeys.get(journeys.size() - 1);
    }
    if (admission() instanceof Admission.Refused refused) {
      throw new Unmet(
          "expected a journey; the workflow was refused: " + refused.violations().getMessage());
    }
    throw new ModelException("the workflow was only admitted, so no journey ran");
  }

  /**
   * The result of the scenario's journey.
   *
   * @throws Unmet when {@code run} refused the journey instead, or no instance was created
   */
  public JourneyResult result() {
    var ended = journey().ended();
    if (ended instanceof Ended.Returned returned) {
      return returned.result();
    }
    if (ended instanceof Ended.Refused refused) {
      throw new Unmet(
          "expected a journey result; run was refused: " + refused.refusal().getMessage());
    }
    if (ended instanceof Ended.Stopped stopped) {
      throw new Unmet(
          "expected a journey result; the test was stopped: " + stopped.mistake().getMessage());
    }
    throw new Unmet(
        "expected a journey result; no instance was created: "
            + ((Ended.NotCreated) ended).failure().getMessage());
  }

  /** The event stream of the last journey that ran; with none, as {@link #stream(Journey)} says. */
  public List<Event> stream() {
    return stream(journeys.isEmpty() ? null : journeys.get(journeys.size() - 1));
  }

  /**
   * The event stream of {@code journey}; with none, everything the held reporter received, or
   * nothing under the default dispatcher.
   */
  public List<Event> stream(Journey journey) {
    return Streams.streamOf(model, recorders, journeys, journey);
  }
}
