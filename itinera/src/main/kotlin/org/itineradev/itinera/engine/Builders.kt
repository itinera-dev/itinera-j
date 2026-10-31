package org.itineradev.itinera.engine

import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowName

/**
 * Starts builders of any mode, which only the core can construct.
 *
 * Part of itinera's engine, not of its API.
 */
public object Builders {
    /**
     * Starts the descriptor of a step of [factory]'s mode.
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <M : Mode> step(name: String, factory: StepFactory<M>): StepBuilder<M> =
        StepBuilder(StepName.of(name), factory)

    /**
     * Starts the descriptor of an input adapter that [call] calls in its mode.
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <W, M : Mode> inputAdapter(
        name: String,
        call: InputAdapterCall<W, M>,
    ): InputAdapterBuilder<W, M> = InputAdapterBuilder(AdapterName.of(name), call)

    /**
     * Starts the descriptor of a workflow of [mode].
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <W, M : Mode> workflow(name: String, mode: M): WorkflowBuilder<W, M> =
        WorkflowBuilder(WorkflowName.of(name), mode)
}
