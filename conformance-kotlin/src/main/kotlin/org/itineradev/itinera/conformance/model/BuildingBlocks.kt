package org.itineradev.itinera.conformance.model

/**
 * The building blocks a scenario defines: its policies, input adapters and service providers. A
 * scenario may define them before the workflow and bind them to it after, or declare them in the
 * workflow directly; the input adapters and providers the workflow declares are those bound, in the
 * order they were bound.
 */
internal class BuildingBlocks {
    /** The policies the scenario defines, by name, in the order first written. */
    val policies = linkedMapOf<String, PolicyModel>()

    /**
     * The input adapters the workflow declares, in the order declared, two of one name among them.
     */
    val adapters = mutableListOf<AdapterModel>()

    /**
     * The service providers the workflow declares, in the order declared, two of one name among
     * them.
     */
    val providers = mutableListOf<ProviderModel>()

    private val unboundAdapters = linkedMapOf<String, AdapterModel>()

    private val unboundProviders = linkedMapOf<String, ProviderModel>()

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

    /** Declares an input adapter attached to [steps], after those already declared. */
    fun adapt(name: String, steps: List<String>) {
        adapters += AdapterModel(name, steps)
    }

    /** Declares a service provider, after those already declared. */
    fun provide(name: String, key: String, type: ServiceType, scope: ProviderScope) {
        providers += ProviderModel(name, key, type, scope)
    }

    /**
     * The one input adapter named [name], declared or defined, defining it, attached to no step
     * yet, when there is none.
     */
    fun definedAdapter(name: String): AdapterModel =
        if (adapters.none { adapter -> isNamed(adapter, name) }) {
            unboundAdapters.getOrPut(name) { AdapterModel(name, emptyList()) }
        } else {
            adapter(name)
        }

    /**
     * Declares the input adapter defined as [name], or a new one when none is defined, attached to
     * [steps], after those already declared.
     */
    fun bindAdapter(name: String, steps: List<String>) {
        val adapter = unboundAdapters.remove(name) ?: AdapterModel(name, emptyList())
        adapter.attach(steps)
        adapters += adapter
    }

    /** Defines a service provider, which a binding then declares, once. */
    fun defineProvider(name: String, key: String, type: ServiceType, scope: ProviderScope) {
        if (name in unboundProviders || providers.any { provider -> isNamed(provider, name) }) {
            throw ModelException("the provider \"$name\" is defined twice")
        }
        unboundProviders[name] = ProviderModel(name, key, type, scope)
    }

    /**
     * Declares the service provider defined as [name], attached to [steps], or to no step when they
     * are `null`, after those already declared.
     */
    fun bindProvider(name: String, steps: List<String>?) {
        val provider =
            unboundProviders.remove(name)
                ?: throw ModelException("no provider \"$name\" is defined to be bound")
        steps?.let(provider::servesOnly)
        providers += provider
    }

    /** The one input adapter declared or defined under [name]. */
    fun adapter(name: String): AdapterModel =
        (adapters + unboundAdapters.values)
            .filter { adapter -> isNamed(adapter, name) }
            .singleOrNull()
            ?: throw ModelException("the workflow declares no adapter, or several, named \"$name\"")

    /** The one provider declared or defined under [name]. */
    fun provider(name: String): ProviderModel =
        (providers + unboundProviders.values)
            .filter { provider -> isNamed(provider, name) }
            .singleOrNull()
            ?: throw ModelException(
                "the workflow declares no provider, or several, named \"$name\""
            )
}

private fun isNamed(adapter: AdapterModel, name: String): Boolean = adapter.name == name

private fun isNamed(provider: ProviderModel, name: String): Boolean = provider.name == name
