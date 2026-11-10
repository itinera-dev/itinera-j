package org.itineradev.itinera.conformance.model

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts,
 * with the building blocks the scenario defines.
 */
internal class WorkflowModel(
    val name: String,
    val steps: List<String>,
    private val blocks: BuildingBlocks,
) {
    /** The script of each step and rescue step, by name; two steps of one name share one. */
    val scripts: MutableMap<String, StepScript> =
        steps.associateWithTo(linkedMapOf()) { StepScript() }

    /** The reporters the workflow lists, in order, once stated. */
    var reporters: List<String>? = null

    /** Where journey IDs come from, once stated. */
    var idGenerator: IdGenerator? = null

    /** The input adapters the workflow declares, in the order declared. */
    val adapters
        get() = blocks.adapters

    /** The service providers the workflow declares, in the order declared. */
    val providers
        get() = blocks.providers

    /** The policies the scenario defines, by name, in the order first written. */
    val policies
        get() = blocks.policies

    /** The workflow policies attached to the workflow, in order, once stated. */
    var workflowPolicies: List<String>? = null
        private set

    /** The roles the workflow provides, each with its operation, by name. */
    val roles = linkedMapOf<String, String>()

    /** The operations of roles that throw when called, as role and operation. */
    val failingOperations = mutableSetOf<Pair<String, String>>()

    /** The policy the scenario defined under [name]. */
    fun policy(name: String): PolicyModel = blocks.policy(name)

    /** Attaches workflow policies, once. */
    fun attach(attached: List<String>) {
        if (workflowPolicies != null)
            throw ModelException("the workflow's policies are stated twice")
        workflowPolicies = attached
    }
}

/** Where a workflow instance's journey ID comes from. */
internal sealed interface IdGenerator {
    /** The default: a UUID v4 for each instance. */
    data object None : IdGenerator

    /** A generator that returns this ID. */
    data class Returns(val id: String) : IdGenerator
}
