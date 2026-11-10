package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HooksReceived.received;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;
import static org.itineradev.itinera.conformance.sentences.Tables.count;
import static org.itineradev.itinera.conformance.sentences.Tables.only;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.ModelException;

/** The table that checks what each hook received, one row for each hook. */
public final class HooksReceivedSentences {
  private static final Map<String, Predicate<HookRequest>> FACT_COLUMNS = factColumns();
  private static final Set<String> COLUMNS = columns();

  private final World world;
  private final HookCalledSentences called;
  private final HookReceivedFactSentences facts;

  /** The table of the scenario sharing {@code world}, read through the sentences it stands for. */
  public HooksReceivedSentences(
      World world, HookCalledSentences called, HookReceivedFactSentences facts) {
    this.world = world;
    this.called = called;
    this.facts = facts;
  }

  @Then("the hooks received:")
  public void theHooksReceived(DataTable table) {
    for (var row : table.asMaps()) {
      hookReceived(row);
    }
  }

  private void hookReceived(Map<String, String> row) {
    only(COLUMNS, row);
    var hook = cell(row, "hook");
    var policy = cell(row, "policy");
    var calls = row.get("calls");
    if (calls != null) {
      called.wasCalled(hook, policy, count(calls));
    }
    for (var column : FACT_COLUMNS.entrySet()) {
      var written = row.get(column.getKey());
      if ("none".equals(written)) {
        received(world, hook, policy, column.getValue(), null);
      } else if (written != null) {
        fact(hook, policy, column.getKey(), written);
      }
    }
  }

  private void fact(String hook, String policy, String column, String written) {
    switch (column) {
      case "attempt number" -> facts.receivedTheAttempt(hook, policy, count(written));
      case "step name" -> facts.receivedTheStepName(hook, policy, written);
      case "journey ID" -> facts.receivedTheJourneyId(hook, policy, written);
      case "cause" -> facts.receivedTheCause(hook, policy, written);
      case "error" -> facts.receivedTheErrorWithTheMessage(hook, policy, written);
      default ->
          throw new ModelException("the column " + column + " holds only none, not " + written);
    }
  }

  private static Map<String, Predicate<HookRequest>> factColumns() {
    var columns = new LinkedHashMap<String, Predicate<HookRequest>>();
    columns.put("attempt number", HookRequests::isAttempt);
    columns.put("step name", HookRequests::isStepName);
    columns.put("journey ID", HookRequests::isJourneyId);
    columns.put("cause", HookRequests::isCause);
    columns.put("error", HookRequests::isError);
    columns.put("failure reason", HookRequests::isReason);
    return columns;
  }

  private static Set<String> columns() {
    var columns = new LinkedHashSet<String>(Set.of("policy", "hook", "calls"));
    columns.addAll(FACT_COLUMNS.keySet());
    return columns;
  }
}
