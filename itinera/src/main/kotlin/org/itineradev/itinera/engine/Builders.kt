package org.itineradev.itinera.engine

import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.report.WorkflowReporter
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.service.ProviderBuilder
import org.itineradev.itinera.service.ProviderCall
import org.itineradev.itinera.step.RescueFactory
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.AdapterName
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.InputAdapterCall
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepBuilder
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowName

/**
 * Starts builders, and makes the reporters workflows list, of any mode, which only the core can
 * construct.
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

    /** Starts the descriptor of a rescue step of [factory]'s mode. */
    public fun <M : Mode> rescueStep(factory: RescueFactory<M>): RescueStepBuilder<M> =
        RescueStepBuilder(factory)

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
     * Starts the descriptor of a service provider of journey scope that [call] calls in its mode.
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <W, M : Mode, T : Any> provider(
        name: String,
        key: String,
        type: Class<T>,
        call: ProviderCall<M, W, JourneyProviderKind<M>, T>,
    ): ProviderBuilder<W, M, JourneyProviderKind<M>> =
        ProviderBuilder.journey(name, key, type, call)

    /**
     * Starts the descriptor of a service provider of attempt scope that [call] calls in its mode.
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <W, M : Mode, T : Any> perAttemptProvider(
        name: String,
        key: String,
        type: Class<T>,
        call: ProviderCall<M, W, AttemptProviderKind<M>, T>,
    ): ProviderBuilder<W, M, AttemptProviderKind<M>> =
        ProviderBuilder.attempt(name, key, type, call)

    /** The reporter [make] makes, of its mode, for each instance of a workflow that lists it. */
    public fun <M : Mode> reporter(make: (JourneyId) -> ReporterOf<M>?): WorkflowReporter<M> =
        WorkflowReporter(make)

    /**
     * Starts the descriptor of a workflow of [mode].
     *
     * @throws IllegalArgumentException when [name] is empty.
     */
    public fun <W, M : Mode> workflow(name: String, mode: M): WorkflowBuilder<W, M> =
        WorkflowBuilder(WorkflowName.of(name), mode)
}
