package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.sentences.Tables.names;

import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.PolicyKind;
import org.itineradev.itinera.conformance.model.RoleOperation;
import org.itineradev.itinera.conformance.model.StepAction;
import org.itineradev.itinera.conformance.model.WorkflowModel;

/**
 * The sentences that define policies and their hooks, attach them, say how they are built, give the
 * workflow its roles, and script what the steps whose handles hooks use do.
 */
public final class PolicySentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public PolicySentences(World world) {
    this.world = world;
  }

  private WorkflowModel workflow() {
    return world.model().workflow();
  }

  @Given("a step policy {string} defines the hook {string}")
  public void definesAStepHook(String policy, String hook) {
    workflow().define(policy, PolicyKind.STEP, Hook.named(hook));
  }

  @Given("a workflow policy {string} defines the hook {string}")
  public void definesAWorkflowHook(String policy, String hook) {
    workflow().define(policy, PolicyKind.WORKFLOW, Hook.named(hook));
  }

  @Given("^step \"([^\"]*)\" has the policies (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void stepHasThePolicies(String step, String policies) {
    world.model().step(step).attach(step, names(policies));
  }

  @Given("^the workflow has the policies (\"[^\"]*\"(?:, \"[^\"]*\")*)$")
  public void workflowHasThePolicies(String policies) {
    workflow().attach(names(policies));
  }

  @Given("the policy {string} fails when it is built")
  public void failsWhenBuilt(String policy) {
    workflow().policy(policy).failsToBuild(null);
  }

  @Given("the policy {string} fails with {string} when it is built")
  public void failsWithWhenBuilt(String policy, String message) {
    workflow().policy(policy).failsToBuild(message);
  }

  @Given("the workflow provides the role {string} with the operation {string}")
  public void providesTheRole(String role, String operation) {
    if (workflow().roles().putIfAbsent(role, operation) != null) {
      throw new ModelException("the role \"" + role + "\" is provided twice");
    }
  }

  @Given("the operation {string} of the role {string} throws")
  public void operationThrows(String operation, String role) {
    workflow().failingOperations().add(new RoleOperation(role, operation));
  }

  @Given("step {string} keeps its contributor and step reporter")
  public void keepsItsHandles(String step) {
    world.model().step(step).keepHandles();
  }

  @Given("^step \"([^\"]*)\" contributes \"([^\"]*)\" = (.+) and then changes it to (.+)$")
  public void contributesThenChanges(String step, String key, String written, String changed) {
    world
        .model()
        .step(step)
        .actions()
        .add(new StepAction.ContributesThenChanges(key, written, value(changed)));
  }
}
