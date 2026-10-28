package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.DispatcherScript;
import org.itineradev.itinera.conformance.model.Dispatching;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ReporterFailure;
import org.itineradev.itinera.event.EventKind;

/**
 * The sentences that declare the workflow's reporters, what the executor is given to dispatch
 * events, and how scripted reporters fail.
 */
public final class ReporterSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ReporterSentences(World world) {
    this.world = world;
  }

  private Model model() {
    return world.model();
  }

  @Given("^the workflow lists the reporters (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void listsTheReporters(String reporters) {
    model().listReporters(names(reporters));
  }

  @Given("the executor uses its default dispatcher")
  public void usesItsDefaultDispatcher() {
    model().dispatchWith(new Dispatching.Default());
  }

  @Given("the executor is given a dispatcher factory that fails")
  public void givenAFactoryThatFails() {
    model().dispatchWith(new Dispatching.FactoryFails());
  }

  @Given("the executor is given a dispatcher holding the reporter {string}")
  public void givenADispatcherHolding(String reporter) {
    model().dispatchWith(new Dispatching.Holding(reporter, new DispatcherScript.Delivers()));
  }

  @Given(
      "the executor is given a dispatcher holding the reporter {string} that ignores added reporters")
  public void thatIgnoresAddedReporters(String reporter) {
    model()
        .dispatchWith(
            new Dispatching.Holding(reporter, new DispatcherScript.IgnoresAddedReporters()));
  }

  @Given(
      "the executor is given a dispatcher holding the reporter {string} that throws when a reporter is added")
  public void thatThrowsWhenAReporterIsAdded(String reporter) {
    model()
        .dispatchWith(new Dispatching.Holding(reporter, new DispatcherScript.ThrowsWhenAdding()));
  }

  @Given(
      "the executor is given a dispatcher holding the reporter {string} that throws when dispatching {string}")
  public void thatThrowsWhenDispatching(String reporter, String event) {
    var script = new DispatcherScript.ThrowsWhenDispatching(kind(event));
    model().dispatchWith(new Dispatching.Holding(reporter, script));
  }

  @Given("the reporter {string} throws")
  public void throwsOnTheFirstEvent(String reporter) {
    model().fails(reporter, new ReporterFailure.OnFirstEvent());
  }

  @Given("the reporter {string} throws on {string}")
  public void throwsOn(String reporter, String event) {
    model().fails(reporter, new ReporterFailure.OnEvent(kind(event), null));
  }

  @Given("the reporter {string} fails with {string} on {string}")
  public void failsWithOn(String reporter, String message, String event) {
    model().fails(reporter, new ReporterFailure.OnEvent(kind(event), message));
  }

  @Given("the reporter {string} fails when it receives the global metadata {string}")
  public void failsOnGlobalMetadata(String reporter, String key) {
    model().fails(reporter, new ReporterFailure.OnGlobalMetadata(key));
  }

  /** The kind of event a case names, by its snake_case name, such as {@code journey_started}. */
  private static EventKind kind(String written) {
    var kind = EventKind.parse(written);
    if (kind == null) {
      throw new ModelException(written + " is not a kind of event");
    }
    return kind;
  }
}
