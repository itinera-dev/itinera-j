package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.sentences.HookScripts.ends;
import static org.itineradev.itinera.conformance.sentences.HookScripts.requests;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;
import static org.itineradev.itinera.conformance.sentences.Tables.isYes;
import static org.itineradev.itinera.conformance.sentences.Tables.only;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import java.util.Set;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.HookReturn;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.PolicyKind;
import org.itineradev.itinera.conformance.model.ValueType;

/**
 * The sentences that define policies and their hooks, and what the hooks request, in tables of one
 * row for each hook or request.
 */
public final class DefinitionSentences {
  private static final Set<String> HOOK_COLUMNS =
      Set.of("policy", "hook", "returns", "code", "fails with");
  private static final Set<String> REQUEST_COLUMNS =
      Set.of("policy", "hook", "request", "key", "type", "optional");

  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public DefinitionSentences(World world) {
    this.world = world;
  }

  @Given("the step policies and their hooks:")
  public void theStepPoliciesAndTheirHooks(DataTable hooks) {
    for (var row : hooks.asMaps()) {
      define(PolicyKind.STEP, row);
    }
  }

  @Given("the workflow policies and their hooks:")
  public void theWorkflowPoliciesAndTheirHooks(DataTable hooks) {
    for (var row : hooks.asMaps()) {
      define(PolicyKind.WORKFLOW, row);
    }
  }

  @Given("the hooks request:")
  public void theHooksRequest(DataTable requests) {
    for (var row : requests.asMaps()) {
      only(REQUEST_COLUMNS, row);
      requests(world, cell(row, "hook"), cell(row, "policy"), requestOf(row));
    }
  }

  private void define(PolicyKind kind, Map<String, String> row) {
    only(HOOK_COLUMNS, row);
    var policy = cell(row, "policy");
    var hook = cell(row, "hook");
    world.model().blocks().define(policy, kind, Hook.named(hook));
    var returns = row.get("returns");
    var failsWith = row.get("fails with");
    if (row.get("code") != null && !"FailWorkflow".equals(returns)) {
      throw new ModelException(
          "the hook \"" + hook + "\" of \"" + policy + "\" has a code but no FailWorkflow");
    }
    if (returns != null) {
      ends(world, hook, policy, returned(returns, row));
    }
    if (failsWith != null) {
      ends(world, hook, policy, new HookReturn.Fails(failsWith));
    }
  }

  private static HookReturn returned(String written, Map<String, String> row) {
    return switch (written) {
      case "FailWorkflow" -> new HookReturn.FailWorkflow(cell(row, "code"));
      case "FinishWorkflow" -> new HookReturn.FinishWorkflow();
      default -> throw new ModelException(written + " is not what a hook of the cases returns");
    };
  }

  private static HookRequest requestOf(Map<String, String> row) {
    var written = row.get("optional");
    var optional = written != null && isYes(written);
    var request = cell(row, "request");
    return switch (request) {
      case "step data" -> new HookRequest.StepData(cell(row, "key"), typeOf(row), optional);
      case "data from the workflow" ->
          new HookRequest.WorkflowData(cell(row, "key"), typeOf(row), optional);
      case "failure reason" -> new HookRequest.FailureReason(optional);
      default -> {
        if (optional) {
          throw new ModelException("the request " + request + " has no optional form");
        }
        yield requiredOf(request);
      }
    };
  }

  private static ValueType typeOf(Map<String, String> row) {
    return ValueType.named(cell(row, "type"));
  }

  private static HookRequest requiredOf(String request) {
    return switch (request) {
      case "failure cause" -> new HookRequest.FailureCause();
      case "retry cause" -> new HookRequest.RetryCause();
      case "error" -> new HookRequest.Error();
      case "step name" -> new HookRequest.StepName();
      case "attempt number" -> new HookRequest.Attempt();
      case "journey ID" -> new HookRequest.JourneyId();
      default -> throw new ModelException(request + " is not a request of a hook of the cases");
    };
  }
}
