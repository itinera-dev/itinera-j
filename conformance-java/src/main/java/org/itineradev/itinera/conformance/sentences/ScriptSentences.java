package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Emitted;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ScriptRow;
import org.itineradev.itinera.conformance.model.ScriptedEnding;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.executor.testing.AttemptRecord;
import org.itineradev.itinera.testing.Mistake;
import org.itineradev.itinera.testing.TestMistake;
import org.itineradev.itinera.value.TypeToken;

/** The sentences of scripted runs: the script, the run, and the run's record. */
public final class ScriptSentences {
  private static final TypeToken<Object> ANY = TypeToken.of(Object.class);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ScriptSentences(World world) {
    this.world = world;
  }

  @Given("the script says:")
  public void scriptSays(DataTable table) {
    world.model().testing().script(table.asMaps().stream().map(ScriptSentences::rowOf).toList());
  }

  @Given("the script ensures every step is scripted")
  public void ensuresEveryStep() {
    world.model().testing().ensureAllScripted();
  }

  @Given("step {string} declares no contributor and no reporter")
  public void declaresNoHandles(String step) {
    world.model().step(step).declareNoHandles();
  }

  @When("the workflow runs with the script")
  public void runsWithTheScript() {
    world.runScripted();
  }

  @Then("^the scripted run's record of step \"([^\"]*)\" holds the attempts (\\d+(?:, \\d+)*)$")
  public void recordHolds(String step, String attempts) {
    var held = attemptsOf(step);
    var expected = Arrays.stream(attempts.split(", ")).map(Integer::valueOf).toList();
    holds(
        held.equals(expected),
        "the attempts " + expected + " of \"" + step + "\"",
        held.toString());
  }

  @Then("the scripted run's record of step {string} holds no attempt")
  public void recordHoldsNone(String step) {
    var held = attemptsOf(step);
    holds(held.isEmpty(), "no attempt of \"" + step + "\"", held.toString());
  }

  @Then("reading the scripted run's record of step {string} stops the test as the mistake {string}")
  public void readingStops(String step, String mistake) {
    try {
      world.scriptedRun().step(step);
    } catch (TestMistake stopped) {
      var problems = stopped.getProblems();
      holds(
          problems.size() == 1 && isNaming(problems.get(0), mistake, step),
          "reading \"" + step + "\" to stop the test as \"" + mistake + "\"",
          stopped.getMessage());
      return;
    }
    throw new Unmet("expected reading \"" + step + "\" to stop the test; it read a record");
  }

  @Then(
      "^attempt (-?\\d+) of step \"([^\"]*)\" in the scripted run's record read the"
          + " (?:input|rescued step data) \"([^\"]*)\" = (.+)$")
  public void attemptRead(int attempt, String step, String key, String written) {
    var record = world.scriptedRun().step(step).attempt(attempt);
    if (record == null) {
      throw new Unmet(
          "expected attempt " + attempt + " of \"" + step + "\" in the record; it holds none");
    }
    var read = record.input(key, ANY).orNull();
    holds(
        Objects.equals(read, value(written)), "\"" + key + "\" = " + written, String.valueOf(read));
  }

  @Then(
      "^reading attempt (-?\\d+) of step \"([^\"]*)\" in the scripted run's record reads nothing$")
  public void readingReadsNothing(int attempt, String step) {
    var record = world.scriptedRun().step(step).attempt(attempt);
    holds(record == null, "no attempt " + attempt + " of \"" + step + "\"", String.valueOf(record));
  }

  private List<Integer> attemptsOf(String step) {
    return world.scriptedRun().step(step).getAttempts().stream()
        .map(ScriptSentences::numberOf)
        .toList();
  }

  private static int numberOf(AttemptRecord record) {
    return record.getAttempt().getNumber();
  }

  private static boolean isNaming(Mistake problem, String mistake, String step) {
    return problem.getKind().toString().equals(mistake)
        && problem instanceof Mistake.UnknownStep unknown
        && unknown.step().equals(step);
  }

  private static ScriptRow rowOf(Map<String, String> row) {
    var emits = row.get("emits");
    return new ScriptRow(
        cell(row, "step"),
        row.get("attempt"),
        objectOf(row.get("contributes")),
        emits == null ? null : Emitted.named(emits),
        row.get("emit message"),
        objectOf(row.get("sets global metadata")),
        ScriptedEnding.named(cell(row, "ending")),
        row.get("code"),
        row.get("message"));
  }

  private static Map<String, Object> objectOf(String written) {
    if (written == null) {
      return Map.of();
    }
    if (!(value(written) instanceof Map<?, ?> object)) {
      throw new ModelException(written + " is not an object");
    }
    var entries = new LinkedHashMap<String, Object>();
    object.forEach((key, entry) -> entries.put((String) key, entry));
    return entries;
  }
}
