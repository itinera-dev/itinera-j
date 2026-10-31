package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.Checked

/**
 * Builds a [WorkflowDescriptor] of mode [M], whose parts it takes only in that mode.
 *
 * @param W the class of the workflow object each instance is created with.
 * @param M the workflow's mode.
 * @sample org.itineradev.itinera.workflow.samples.building
 */
@ExperimentalItineraApi
public class WorkflowBuilder<W, M : Mode>
internal constructor(private val name: WorkflowName, private val mode: M) {
    private val steps = mutableListOf<StepDescriptor<M>>()
    private var idGenerator: JourneyIdGenerator<W>? = null

    /**
     * Adds a step after those already added.
     *
     * @sample org.itineradev.itinera.workflow.samples.building
     */
    public fun step(step: StepDescriptor<M>): WorkflowBuilder<W, M> = apply { steps += step }

    /**
     * Produces each instance's journey ID with this generator, in place of a UUID version 4.
     *
     * @sample org.itineradev.itinera.workflow.samples.ids
     */
    public fun idGenerator(generator: JourneyIdGenerator<W>): WorkflowBuilder<W, M> = apply {
        idGenerator = generator
    }

    /**
     * The descriptor, once every violation is checked.
     *
     * @throws Violations with every violation found.
     * @sample org.itineradev.itinera.workflow.samples.building
     */
    public fun build(): WorkflowDescriptor<W, M> = Checked.of(name, mode, steps, idGenerator)
}
