package org.itineradev.itinera.conformance.model

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts.
 */
internal class WorkflowModel(val name: String, val steps: List<String>) {
    /** The script of each step and rescue step, by name; two steps of one name share one. */
    val scripts: MutableMap<String, StepScript> =
        steps.associateWithTo(linkedMapOf()) { StepScript() }

    /** The reporters the workflow lists, in order, once stated. */
    var reporters: List<String>? = null

    /** Where journey IDs come from, once stated. */
    var idGenerator: IdGenerator? = null

    /** The input adapters, in the order declared, two of one name among them. */
    val adapters = mutableListOf<AdapterModel>()

    /** The service providers, in the order declared, two of one name among them. */
    val providers = mutableListOf<ProviderModel>()

    /** The policies the scenario defines, by name, in the order first written. */
    val policies = linkedMapOf<String, PolicyModel>()

    /** The workflow policies attached to the workflow, in order, once stated. */
    var workflowPolicies: List<String>? = null
        private set

    /** The roles the workflow provides, each with its operation, by name. */
    val roles = linkedMapOf<String, String>()

    /** The operations of roles that throw when called, as role and operation. */
    val failingOperations = mutableSetOf<Pair<String, String>>()

    /** Defines [hook] on the policy [name] of [kind], defining the policy the first time. */
    fun define(name: String, kind: PolicyKind, hook: Hook) {
        val policy = policies.getOrPut(name) { PolicyModel(name, kind) }
        if (policy.kind != kind) throw ModelException("\"$name\" is a step and a workflow policy")
        if (hook in policy.hooks) throw ModelException("\"$name\" defines \"$hook\" twice")
        policy.hooks[hook] = HookScript()
    }

    /** The policy the scenario defined under [name]. */
    fun policy(name: String): PolicyModel =
        policies[name] ?: throw ModelException("no policy \"$name\" is defined")

    /** Attaches workflow policies, once. */
    fun attach(attached: List<String>) {
        if (workflowPolicies != null)
            throw ModelException("the workflow's policies are stated twice")
        workflowPolicies = attached
    }

    /** Declares an input adapter attached to [steps], after those already declared. */
    fun adapt(name: String, steps: List<String>) {
        adapters += AdapterModel(name, steps)
    }

    /** Declares a service provider, after those already declared. */
    fun provide(name: String, key: String, type: ServiceType, scope: ProviderScope) {
        providers += ProviderModel(name, key, type, scope)
    }

    /** The one input adapter the workflow declares under [name]. */
    fun adapter(name: String): AdapterModel =
        adapters.filter { adapter -> isNamed(adapter, name) }.singleOrNull()
            ?: throw ModelException("the workflow declares no adapter, or several, named \"$name\"")

    /** The one provider the workflow declares under [name]. */
    fun provider(name: String): ProviderModel =
        providers.filter { provider -> isNamed(provider, name) }.singleOrNull()
            ?: throw ModelException(
                "the workflow declares no provider, or several, named \"$name\""
            )
}

private fun isNamed(adapter: AdapterModel, name: String): Boolean = adapter.name == name

private fun isNamed(provider: ProviderModel, name: String): Boolean = provider.name == name

/** Where a workflow instance's journey ID comes from. */
internal sealed interface IdGenerator {
    /** The default: a UUID v4 for each instance. */
    data object None : IdGenerator

    /** A generator that returns this ID. */
    data class Returns(val id: String) : IdGenerator
}
