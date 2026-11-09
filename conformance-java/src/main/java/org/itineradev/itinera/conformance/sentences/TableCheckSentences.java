package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.cell;
import static org.itineradev.itinera.conformance.sentences.Tables.count;
import static org.itineradev.itinera.conformance.sentences.Tables.only;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.Map;
import java.util.Set;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.trace.Status;

/**
 * The tables that check how the steps ended, what they were built with and given, and what the
 * reporters received, each row standing for the sentence of its kind.
 */
public final class TableCheckSentences {
  private static final Set<String> ENDED_COLUMNS = Set.of("step", "status", "attempts");
  private static final Set<String> SERVICES_COLUMNS = Set.of("step", "service", "label", "attempt");
  private static final Set<String> BUILT_COLUMNS = Set.of("step", "attempt", "input", "value");
  private static final Set<String> REPORTERS_COLUMNS = Set.of("reporter", "event", "received");

  private final StepSentences steps;
  private final BuiltSentences built;
  private final ReceivedSentences received;

  /** The tables of the scenario, read through the sentences each row stands for. */
  public TableCheckSentences(
      StepSentences steps, BuiltSentences built, ReceivedSentences received) {
    this.steps = steps;
    this.built = built;
    this.received = received;
  }

  @Then("the steps ended as:")
  public void theStepsEndedAs(DataTable table) {
    for (var row : table.asMaps()) {
      ended(row);
    }
  }

  @Then("the steps were given the services:")
  public void theStepsWereGivenTheServices(DataTable table) {
    for (var row : table.asMaps()) {
      only(SERVICES_COLUMNS, row);
      built.wasGivenTheService(
          cell(row, "step"), cell(row, "service"), cell(row, "label"), count(cell(row, "attempt")));
    }
  }

  @Then("the steps were built with:")
  public void theStepsWereBuiltWith(DataTable table) {
    for (var row : table.asMaps()) {
      builtWith(row);
    }
  }

  @Then("the reporters received:")
  public void theReportersReceived(DataTable table) {
    for (var row : table.asMaps()) {
      reporterReceived(row);
    }
  }

  private void ended(Map<String, String> row) {
    only(ENDED_COLUMNS, row);
    var step = cell(row, "step");
    var status = Status.named(cell(row, "status"));
    var attempts = row.get("attempts");
    if (attempts != null) {
      steps.endedAsAfter(step, status, count(attempts));
    } else if (status == Status.ABORTED) {
      steps.endedAsAborted(step);
    } else {
      throw new ModelException(
          "the step \"" + step + "\" ended as " + status.written() + " with no attempts");
    }
  }

  private void builtWith(Map<String, String> row) {
    only(BUILT_COLUMNS, row);
    var step = cell(row, "step");
    var input = cell(row, "input");
    var value = cell(row, "value");
    var attempt = row.get("attempt");
    if (value.equals("absent") && attempt == null) {
      built.wasBuiltWithInputAbsent(step, input);
    } else if (value.equals("absent")) {
      throw new ModelException("an input absent is checked for attempt 1 only");
    } else if (attempt == null) {
      built.wasBuiltWithInput(step, input, value);
    } else {
      built.wasBuiltForAttemptWithInput(step, count(attempt), input, value);
    }
  }

  private void reporterReceived(Map<String, String> row) {
    only(REPORTERS_COLUMNS, row);
    var reporter = cell(row, "reporter");
    var event = cell(row, "event");
    var written = cell(row, "received");
    switch (written) {
      case "yes" -> received.receivedTheEvent(reporter, event);
      case "no" -> received.didNotReceive(reporter, event);
      default -> received.receivedTheEventTimes(reporter, event, count(written));
    }
  }
}
