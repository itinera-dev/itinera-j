package org.itineradev.itinera.workflow

import org.itineradev.itinera.engine.Described
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.RescueKind
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.SyncRescueFactory

/**
 * How the rescue step of a step of a workflow of mode [M] is declared: what it requests and how it
 * is built. That is all: a rescue step is named after the step it rescues, `<step name>_rescue`,
 * and has no policies, no retry budget and no `retry on abnormal termination` of its own. When an
 * attempt of its step fails, it runs once, and if it succeeds, the step is attempted once more; a
 * step is rescued at most once per journey.
 *
 * [builder] makes one for a synchronous workflow, [of] reads one from a class marked `@RescueStep`,
 * and `asyncRescueStep` and `asyncRescueStepOf` from `itinera-kotlin` do the same for an
 * asynchronous one. A step descriptor's builder attaches it to its step.
 *
 * @param M the mode of the workflows the rescue step can join.
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
public interface RescueStepDescriptor<M : Mode> {
    /**
     * What the rescue step requests, in the order declared, which its factory reads: its inputs and
     * what it requests from the step it rescues are resolved first, in this order, then its
     * services.
     */
    public val requests: List<Request<RescueKind<M>, *>>

    /** How the rescue step is built. */
    public val factory: RescueFactory<M>

    /** Makes the descriptors of rescue steps. */
    public companion object {
        /**
         * Starts the descriptor of a synchronous rescue step, which the factory builds.
         *
         * @sample org.itineradev.itinera.workflow.samples.rescues
         */
        @JvmStatic
        public fun builder(factory: SyncRescueFactory): RescueStepBuilder<Sync> =
            RescueStepBuilder(factory)

        /**
         * The descriptor of a synchronous rescue step written as a class marked `@RescueStep`,
         * built with its constructor and run with its method `run`, which request what the rescue
         * step requests. A Kotlin class needs `itinera-kotlin` to be read.
         *
         * @throws Violations with every mistake in the class's declaration.
         * @sample org.itineradev.itinera.workflow.annotated.samples.describingARescueStep
         */
        @JvmStatic
        public fun of(type: Class<out ItineraRescueStep>): RescueStepDescriptor<Sync> =
            Described.synchronousPartClasses.rescue(type)
    }
}

/**
 * Builds a [RescueStepDescriptor] of mode [M].
 *
 * @param M the mode of the workflows the rescue step can join.
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
public class RescueStepBuilder<M : Mode>
internal constructor(private val factory: RescueFactory<M>) {
    private val requests = mutableListOf<Request<RescueKind<M>, *>>()

    /**
     * Declares a request after those already declared, which the rescue step's factory then reads.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public fun request(request: Request<RescueKind<M>, *>): RescueStepBuilder<M> = apply {
        requests += request
    }

    /**
     * The descriptor.
     *
     * @sample org.itineradev.itinera.workflow.samples.rescues
     */
    public fun build(): RescueStepDescriptor<M> = Described(requests.toList(), factory)

    private class Described<M : Mode>(
        override val requests: List<Request<RescueKind<M>, *>>,
        override val factory: RescueFactory<M>,
    ) : RescueStepDescriptor<M>
}
