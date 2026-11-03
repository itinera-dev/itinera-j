package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.PolicyKind
import org.itineradev.itinera.conformance.model.StepAction
import org.itineradev.itinera.conformance.model.value

/**
 * The sentences that define policies and their hooks, attach them, say how they are built, give the
 * workflow its roles, and script what the steps whose handles hooks use do.
 */
internal class PolicySentences(private val world: World) {
    private val workflow
        get() = world.model.workflow

    @Given("a step policy {string} defines the hook {string}")
    fun definesAStepHook(policy: String, hook: String) {
        workflow.define(policy, PolicyKind.STEP, Hook.named(hook))
    }

    @Given("a workflow policy {string} defines the hook {string}")
    fun definesAWorkflowHook(policy: String, hook: String) {
        workflow.define(policy, PolicyKind.WORKFLOW, Hook.named(hook))
    }

    @Given("""^step "([^"]*)" has the policies ("[^"]*"(?:, "[^"]*")*)$""")
    fun stepHasThePolicies(step: String, policies: String) {
        world.model.step(step).attach(step, names(policies))
    }

    @Given("""^the workflow has the policies ("[^"]*"(?:, "[^"]*")*)$""")
    fun workflowHasThePolicies(policies: String) {
        workflow.attach(names(policies))
    }

    @Given("the policy {string} fails when it is built")
    fun failsWhenBuilt(policy: String) {
        workflow.policy(policy).failsToBuild(null)
    }

    @Given("the policy {string} fails with {string} when it is built")
    fun failsWithWhenBuilt(policy: String, message: String) {
        workflow.policy(policy).failsToBuild(message)
    }

    @Given("the workflow provides the role {string} with the operation {string}")
    fun providesTheRole(role: String, operation: String) {
        if (role in workflow.roles) throw ModelException("the role \"$role\" is provided twice")
        workflow.roles[role] = operation
    }

    @Given("the operation {string} of the role {string} throws")
    fun operationThrows(operation: String, role: String) {
        workflow.failingOperations += role to operation
    }

    @Given("step {string} keeps its contributor and step reporter")
    fun keepsItsHandles(step: String) {
        world.model.step(step).keepsHandles = true
    }

    @Given("""^step "([^"]*)" contributes "([^"]*)" = (.+) and then changes it to (.+)$""")
    fun contributesThenChanges(step: String, key: String, written: String, changed: String) {
        world.model.step(step).actions +=
            StepAction.ContributesThenChanges(key, written, value(changed))
    }
}
