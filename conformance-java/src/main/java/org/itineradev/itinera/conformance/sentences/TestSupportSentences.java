package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.FakeModel;
import org.itineradev.itinera.conformance.model.Implementation;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.testing.Mistake;

/**
 * The sentences of test support's recording reporter, its fakes, and the mistakes it stops a test
 * for.
 */
public final class TestSupportSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public TestSupportSentences(World world) {
    this.world = world;
  }

  @Given("the instance has the recording reporter of test support")
  public void hasTheRecordingReporter() {
    world.model().testing().record();
  }

  @Then("the path is:")
  public void pathIs(DataTable table) {
    var path = world.tested().kept().path();
    var expected = table.asMaps().stream().map(row -> cell(row, "line")).toList();
    holds(path.equals(expected), "the path " + expected, path.toString());
  }

  @Then("the path is empty")
  public void pathIsEmpty() {
    var path = world.tested().kept().path();
    holds(path.isEmpty(), "an empty path", path.toString());
  }

  @Then("the recording reporter of test support kept the same events as the reporter {string}")
  public void keptTheSameEventsAs(String reporter) {
    var kept = world.tested().kept().events();
    var made = world.recorders().made(reporter);
    if (made.isEmpty()) {
      throw new ModelException("no reporter \"" + reporter + "\" was made");
    }
    var received = made.get(made.size() - 1).events();
    holds(
        kept.equals(received), "the events of \"" + reporter + "\", " + received, kept.toString());
  }

  @Then("the recording reporter of test support kept the global metadata:")
  public void keptTheGlobalMetadata(DataTable table) {
    var kept = new ArrayList<>(world.tested().kept().globalMetadata().entrySet());
    var expected = table.asMaps().stream().map(Tables::entryOf).toList();
    holds(kept.equals(expected), "the global metadata " + expected, kept.toString());
  }

  @Given(
      "the test replaces the service provider {string} with a fake that makes instances of {implementation}")
  public void fakeMakes(String provider, Implementation implementation) {
    world.model().testing().fake(provider, new FakeModel.Makes(implementation, null));
  }

  @Given(
      "^the test replaces the service provider \"([^\"]*)\" with a fake that makes instances of"
          + " (first labeller|second labeller) and supplies the data (.+)$")
  public void fakeMakesWithData(String provider, String implementation, String data) {
    world
        .model()
        .testing()
        .fake(provider, new FakeModel.Makes(Implementation.named(implementation), value(data)));
  }

  @Given("the test replaces the service provider {string} with a fake that fails with {string}")
  public void fakeFails(String provider, String message) {
    world.model().testing().fake(provider, new FakeModel.Fails(message));
  }

  @Given("the test replaces the service provider {string} with a fake that makes meters")
  public void fakeMakesMeters(String provider) {
    world.model().testing().fake(provider, new FakeModel.MakesMeters());
  }

  @Then("the fake of {string} was called {int} times")
  public void fakeWasCalled(String provider, int times) {
    var fake = world.tested().fakes().get(provider);
    if (fake == null) {
      throw new ModelException("no fake replaces the provider \"" + provider + "\"");
    }
    holds(
        fake.calls() == times,
        "the fake of \"" + provider + "\" called " + times + " times",
        fake.calls() + " calls");
  }

  @Then("the test stopped before the journey, naming:")
  public void stoppedBeforeTheJourney(DataTable table) {
    var journeys = world.journeys();
    var ended = journeys.isEmpty() ? null : journeys.get(journeys.size() - 1).ended();
    if (!(ended instanceof Ended.Stopped stopped)) {
      throw new Unmet("expected the test to stop before the journey; it ended " + ended);
    }
    var mistake = stopped.mistake();
    var found = sorted(mistake.getProblems().stream().map(TestSupportSentences::namedBy).toList());
    var expected = sorted(table.asMaps().stream().map(TestSupportSentences::namedIn).toList());
    holds(found.equals(expected), "the mistakes " + expected, mistake.getMessage());
  }

  /**
   * What a row of mistakes names.
   *
   * @param mistake the mistake, as test support words it
   * @param step the step it names, if any
   * @param attempt the attempt it names, if any
   * @param provider the provider it names, if any
   */
  private record Named(String mistake, String step, String attempt, String provider) {}

  private static List<Named> sorted(List<Named> named) {
    return named.stream().sorted(Comparator.comparing(Named::toString)).toList();
  }

  private static Named namedIn(Map<String, String> row) {
    return new Named(
        cell(row, "mistake"), row.get("step"), row.get("attempt"), row.get("provider"));
  }

  /** What {@code mistake} names, its kind as test support words it. */
  private static Named namedBy(Mistake mistake) {
    var kind = mistake.getKind().toString();
    if (mistake instanceof Mistake.GapInTheAttempts gap) {
      return new Named(kind, gap.step(), String.valueOf(gap.attempt()), null);
    }
    if (mistake instanceof Mistake.AttemptScriptedTwice twice) {
      return new Named(kind, twice.step(), String.valueOf(twice.attempt()), null);
    }
    if (mistake instanceof Mistake.NoSuchAttempt none) {
      return new Named(kind, none.step(), String.valueOf(none.attempt()), null);
    }
    if (mistake instanceof Mistake.AttemptLeftOut left) {
      return new Named(kind, left.step(), String.valueOf(left.attempt()), null);
    }
    if (mistake instanceof Mistake.UnknownProvider unknown) {
      return new Named(kind, null, null, unknown.provider());
    }
    if (mistake instanceof Mistake.AnotherService another) {
      return new Named(kind, null, null, another.provider());
    }
    return new Named(kind, stepOf(mistake), null, null);
  }

  private static String stepOf(Mistake mistake) {
    if (mistake instanceof Mistake.UnknownStep unknown) {
      return unknown.step();
    }
    if (mistake instanceof Mistake.StepScriptedTwice twice) {
      return twice.step();
    }
    if (mistake instanceof Mistake.RescueStepSkips skips) {
      return skips.step();
    }
    if (mistake instanceof Mistake.NoContributor contributor) {
      return contributor.step();
    }
    if (mistake instanceof Mistake.NoReporter reporter) {
      return reporter.step();
    }
    if (mistake instanceof Mistake.RescueStepLeftOut rescue) {
      return rescue.step();
    }
    return ((Mistake.StepLeftOut) mistake).step();
  }
}
