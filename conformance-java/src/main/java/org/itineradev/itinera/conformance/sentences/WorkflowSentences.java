package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Ending;
import org.itineradev.itinera.conformance.model.Input;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.itineradev.itinera.conformance.model.ValueType;
import org.itineradev.itinera.conformance.run.NotAValue;

/**
 * The sentences that declare the workflow, its steps and rescue steps, what each step requests, how
 * each attempt ends and how often a step may retry.
 */
public final class WorkflowSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public WorkflowSentences(World world) {
    this.world = world;
  }

  private Model model() {
    return world.model();
  }

  @Given("a workflow {string} with the steps:")
  public void aWorkflowWithTheSteps(String workflow, DataTable steps) {
    model().declare(workflow, steps.asMaps().stream().map(WorkflowSentences::stepOf).toList());
  }

  @Given("step {string} requests input {string} of type {word}")
  public void requestsInput(String step, String key, String type) {
    model().step(step).inputs().add(new Input(key, ValueType.named(type), false));
  }

  @Given("step {string} requests optional input {string} of type {word}")
  public void requestsOptionalInput(String step, String key, String type) {
    model().step(step).inputs().add(new Input(key, ValueType.named(type), true));
  }

  @Given("step {string} succeeds")
  public void succeeds(String step) {
    model().step(step).attempts(step, List.of(new ScriptedAttempt(1, Ending.SUCCESS)));
  }

  @Given("step {string} fails with code {string} and details that are not a value")
  public void failsWithANonValue(String step, String code) {
    model()
        .step(step)
        .attempts(
            step,
            List.of(
                new ScriptedAttempt(1, Ending.FAILURE, code, null, NotAValue.INSTANCE, Map.of())));
  }

  @Given("step {string} attempts:")
  public void attempts(String step, DataTable attempts) {
    model().step(step).attempts(step, attempts.asMaps().stream().map(Tables::attemptOf).toList());
  }

  @Given("step {string} cannot be built because its constructor fails with {string}")
  public void cannotBeBuilt(String step, String message) {
    model().step(step).cannotBeBuilt(step, message);
  }

  @Given("step {string} does not retry abnormal terminations")
  public void doesNotRetryAbnormalTerminations(String step) {
    model().step(step).doesNotRetryAbnormalTerminations();
  }

  @Given("step {string} allows {int} retries")
  public void allowsRetries(String step, int retries) {
    model().step(step).allows(step, retries);
  }

  @Given("step {string} has a rescue step")
  public void hasARescueStep(String step) {
    model().rescue(step);
  }

  private static String stepOf(Map<String, String> row) {
    return cell(row, "step");
  }
}
