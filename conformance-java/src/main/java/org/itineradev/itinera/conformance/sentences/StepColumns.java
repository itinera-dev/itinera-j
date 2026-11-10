package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.Tables.cell;
import static org.itineradev.itinera.conformance.sentences.Tables.isYes;
import static org.itineradev.itinera.conformance.sentences.Tables.only;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.itineradev.itinera.conformance.model.Ending;
import org.itineradev.itinera.conformance.model.Input;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.RequestedService;
import org.itineradev.itinera.conformance.model.ScriptedAttempt;
import org.itineradev.itinera.conformance.model.ServiceType;
import org.itineradev.itinera.conformance.model.ValueType;

/** What the columns of the workflow's steps table stand for, for the step of each row. */
final class StepColumns {
  private static final Set<String> COLUMNS =
      Set.of("step", "policies", "inputs", "services", "outcome", "code", "rescue");

  private StepColumns() {}

  /**
   * Scripts the step of one row of the steps table with what its filled cells stand for: its
   * policies, its inputs and services, how every attempt ends and whether it has a rescue step.
   */
  static void script(Model model, Map<String, String> row) {
    only(COLUMNS, row);
    var step = cell(row, "step");
    var script = model.step(step);
    var policies = row.get("policies");
    var inputs = row.get("inputs");
    var services = row.get("services");
    var outcome = row.get("outcome");
    var rescue = row.get("rescue");
    if (policies != null) {
      script.attach(step, listed(policies));
    }
    if (inputs != null) {
      script.inputs().addAll(listed(inputs).stream().map(StepColumns::inputOf).toList());
    }
    if (services != null) {
      script.services().addAll(listed(services).stream().map(StepColumns::serviceOf).toList());
    }
    if (outcome != null) {
      script.attempts(step, List.of(everyAttempt(outcome, row)));
    }
    if (row.get("code") != null && (outcome == null || Ending.named(outcome) != Ending.FAILURE)) {
      throw new ModelException("the step \"" + step + "\" has a code but no failure");
    }
    if (rescue != null && isYes(rescue)) {
      model.rescue(step);
    }
  }

  /** The items of a cell, separated by commas. */
  private static List<String> listed(String written) {
    return Arrays.stream(written.split(",")).map(String::trim).toList();
  }

  private static Input inputOf(String written) {
    var keyed = keyed(written);
    return new Input(keyed.get(0), ValueType.named(keyed.get(1)), false);
  }

  private static RequestedService serviceOf(String written) {
    var keyed = keyed(written);
    return new RequestedService(keyed.get(0), ServiceType.named(keyed.get(1)));
  }

  /** A request written {@code key: type}, as its key and its type. */
  private static List<String> keyed(String written) {
    var halves = Arrays.stream(written.split(":")).map(String::trim).toList();
    if (halves.size() != 2) {
      throw new ModelException(written + " is not written as key: type");
    }
    return halves;
  }

  private static ScriptedAttempt everyAttempt(String outcome, Map<String, String> row) {
    var ending = Ending.named(outcome);
    return switch (ending) {
      case SUCCESS -> new ScriptedAttempt(1, ending);
      case FAILURE -> new ScriptedAttempt(1, ending, cell(row, "code"), null, null, Map.of());
      case SKIPPED, ERROR ->
          throw new ModelException(outcome + " is not an outcome of the steps table");
    };
  }
}
