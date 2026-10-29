package org.itineradev.itinera.instance.testing

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.service.AttemptProviderDescriptor
import org.itineradev.itinera.service.JourneyProviderDescriptor
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.testing.Mistake
import org.itineradev.itinera.testing.TestMistake
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** How a fake of mode [M] is called in place of a provider's call. */
internal interface FakeCall<M : Mode> {
    /** The call of a provider of kind [K], called with a workflow object of class [W]. */
    fun <W, K> call(): ProviderCall<M, W, K, *>
}

/**
 * The fakes a test puts in place of the service providers of [descriptor], by the name of the
 * provider each replaces.
 */
internal class Fakes<M : Mode>(private val descriptor: WorkflowDescriptor<*, M>) {
    private val calls = LinkedHashMap<ProviderName, FakeCall<M>>()

    /**
     * Puts [call] in place of the provider named [name], which makes [service], replacing the fake
     * put there before.
     *
     * @throws TestMistake when the workflow has no provider named [name], or it makes another
     *   service than [service].
     */
    fun put(name: String, service: Class<*>, call: FakeCall<M>) {
        val provider =
            descriptor.providers.find { provider -> isNamed(provider, name) }
                ?: throw TestMistake(listOf(Mistake.UnknownProvider(name)))
        if (provider.type != service) throw TestMistake(listOf(Mistake.AnotherService(name)))
        calls[provider.name] = call
    }

    /** [instance], whose providers the fakes replace. */
    fun of(instance: WorkflowInstance<M>): WorkflowInstance<M> =
        if (calls.isEmpty()) instance else FakedInstance(instance, faked(instance.descriptor))

    private fun <W> faked(real: WorkflowDescriptor<W, M>): WorkflowDescriptor<W, M> =
        FakedWorkflow(real, real.providers.map(::faked))

    private fun <W> faked(provider: ProviderDescriptor<W, M>): ProviderDescriptor<W, M> {
        val fake = calls[provider.name] ?: return provider
        return when (provider) {
            is JourneyProviderDescriptor -> FakedJourneyProvider(provider, fake.call())
            is AttemptProviderDescriptor -> FakedAttemptProvider(provider, fake.call())
        }
    }
}

private fun isNamed(provider: ProviderDescriptor<*, *>, name: String): Boolean =
    provider.name.toString() == name

/** The workflow [real] describes, with [providers] in place of its own. */
private class FakedWorkflow<W, M : Mode>(
    private val real: WorkflowDescriptor<W, M>,
    override val providers: List<ProviderDescriptor<W, M>>,
) : WorkflowDescriptor<W, M> by real

/** The provider [real] describes, called through [call]. */
private class FakedJourneyProvider<W, M : Mode>(
    private val real: JourneyProviderDescriptor<W, M>,
    override val call: ProviderCall<M, W, JourneyProviderKind<M>, *>,
) : JourneyProviderDescriptor<W, M> by real

/** The provider [real] describes, called through [call]. */
private class FakedAttemptProvider<W, M : Mode>(
    private val real: AttemptProviderDescriptor<W, M>,
    override val call: ProviderCall<M, W, AttemptProviderKind<M>, *>,
) : AttemptProviderDescriptor<W, M> by real

/** [real], whose descriptor is [descriptor]. */
private class FakedInstance<M : Mode>(
    private val real: WorkflowInstance<M>,
    override val descriptor: WorkflowDescriptor<*, M>,
) : WorkflowInstance<M> by real
