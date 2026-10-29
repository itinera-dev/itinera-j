package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.run.Alone;
import org.itineradev.itinera.conformance.run.Call;
import org.itineradev.itinera.conformance.run.RanAlone;
import org.itineradev.itinera.report.testing.Emitted;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.Supplied;

/**
 * The sentences of test support's recorders, which lend a step, a hook or an input adapter its
 * handles outside a journey, and what they kept.
 */
public final class AloneSentences {
  private static final TypeToken<Object> ANY = TypeToken.of(Object.class);

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public AloneSentences(World world) {
    this.world = world;
  }

  @When("step {string} runs alone")
  public void stepRunsAlone(String step) {
    world.ranAlone(Alone.stepRunAlone(world.model(), step));
  }

  @When("the hook {string} of policy {string} runs alone")
  public void hookRunsAlone(String hook, String policy) {
    world.ranAlone(Alone.hookRunAlone(world.model(), world.witness(), policy, hook));
  }

  @When(
      "the input adapter {string} is called alone for the input {string} of step {string}, with a"
          + " data bag holding:")
  public void adapterIsCalledAlone(String adapter, String input, String step, DataTable table) {
    var values = table.asMaps().stream().map(Tables::entryOf).toList();
    world.returned(Alone.adapterCalledAlone(world.model(), adapter, step, input, values));
  }

  @Given("the step event reporter is failing")
  public void stepRecorderFails() {
    world.model().testing().failStepRecorder();
  }

  @Given("the hook event reporter is failing")
  public void hookRecorderFails() {
    world.model().testing().failHookRecorder();
  }

  @Then("the contributions recorder holds:")
  public void contributionsHold(DataTable table) {
    var contributions = world.ranAlone().contributions();
    var expected = table.asMaps().stream().map(Tables::entryOf).toList();
    for (var entry : expected) {
      var read = contributions.read(entry.getKey(), ANY).orNull();
      holds(
          Objects.equals(read, entry.getValue()),
          "\"" + entry.getKey() + "\" = " + entry.getValue() + " in the contributions recorder",
          String.valueOf(read));
    }
    var keys = contributions.getKeys();
    var expectedKeys = expected.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    holds(keys.equals(expectedKeys), "the keys of " + expected, keys.toString());
  }

  @Then("^the (step|hook) event reporter kept the events:$")
  public void keptTheEvents(String recorder, DataTable table) {
    var kept = keptBy(recorder).emitted().stream().map(AloneSentences::written).toList();
    var expected = table.asMaps().stream().map(AloneSentences::expectedEmitted).toList();
    holds(
        kept.equals(expected),
        "the " + recorder + " event reporter to keep " + expected,
        kept.toString());
  }

  @Then("^the (step|hook) event reporter kept nothing$")
  public void keptNothing(String recorder) {
    var ran = keptBy(recorder);
    holds(
        ran.emitted().isEmpty() && ran.globalMetadata().isEmpty(),
        "the " + recorder + " event reporter to keep nothing",
        ran.emitted() + " and " + ran.globalMetadata());
  }

  @Then("the step event reporter kept the global metadata:")
  public void keptTheGlobalMetadata(DataTable table) {
    var kept = new ArrayList<>(keptBy("step").globalMetadata().entrySet());
    var expected = table.asMaps().stream().map(Tables::entryOf).toList();
    holds(kept.equals(expected), "the global metadata " + expected, kept.toString());
  }

  @Then(
      "^every call (step \"[^\"]*\"|the hook \"[^\"]*\" of policy \"[^\"]*\") made to its"
          + " (?:step|hook) reporter was interrupted$")
  public void everyCallInterrupted(String called) {
    var calls = calledAlone(called).calls();
    holds(
        !calls.isEmpty() && calls.stream().allMatch(Call::interrupted),
        "every call to the reporter to be interrupted",
        calls.toString());
  }

  @Then(
      "the call step {string} made setting {string}, and every call after it, was interrupted,"
          + " and no call before it")
  public void interruptedFromSetting(String step, String key) {
    var calls = calledAlone("step \"" + step + "\"").calls();
    var setting =
        IntStream.range(0, calls.size())
            .filter(index -> isSettingAt(calls, index, key))
            .findFirst()
            .orElseThrow(
                () -> new ModelException("step \"" + step + "\" never set \"" + key + "\""));
    holds(
        calls.subList(0, setting).stream().noneMatch(Call::interrupted)
            && calls.subList(setting, calls.size()).stream().allMatch(Call::interrupted),
        "the call setting \""
            + key
            + "\", and every call after it, to be interrupted, and no call"
            + " before it",
        calls.toString());
  }

  @Then("the input adapter returned {}")
  public void adapterReturned(String written) {
    var returned = world.returned();
    var expected = value(written);
    holds(
        returned instanceof Supplied.Value supplied && Objects.equals(supplied.value(), expected),
        "the input adapter to return " + written,
        returned.toString());
  }

  private RanAlone keptBy(String recorder) {
    var ran = world.ranAlone();
    if (!ran.recorder().equals(recorder)) {
      throw new ModelException(
          "the " + recorder + " event reporter lent nothing; " + ran.called() + " ran alone");
    }
    return ran;
  }

  private static boolean isSettingAt(List<Call> calls, int index, String key) {
    var call = calls.get(index);
    return call.method().equals(Call.SETTING) && call.key().equals(key);
  }

  private RanAlone calledAlone(String called) {
    var ran = world.ranAlone();
    if (!ran.called().equals(called)) {
      throw new ModelException(called + " did not run alone");
    }
    return ran;
  }

  /**
   * An event a recorder kept, as a table writes it.
   *
   * @param level its level
   * @param message its message
   * @param data its data, {@code null} for none
   */
  private record WrittenEmitted(String level, String message, Object data) {}

  private static WrittenEmitted written(Emitted emitted) {
    return new WrittenEmitted(
        emitted.level().name().toLowerCase(Locale.ROOT), emitted.message(), emitted.data());
  }

  private static WrittenEmitted expectedEmitted(Map<String, String> row) {
    var data = row.get("data");
    return new WrittenEmitted(
        cell(row, "level"),
        cell(row, "message"),
        data == null || data.equals("none") ? null : value(data));
  }
}
