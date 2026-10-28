package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import org.itineradev.itinera.step.StepFactory
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The descriptors of workflows written as annotated classes, each read once per class and mode.
 *
 * Part of itinera's engine, not of its API.
 */
public object Described {
    private val synchronous =
        object : ClassValue<WorkflowDescriptor<*, Sync>>() {
            override fun computeValue(type: Class<*>): WorkflowDescriptor<*, Sync> =
                read<Any?, Sync>(type, Sync, ::synchronousStep)
        }

    /**
     * The descriptor of the synchronous workflow [type] declares.
     *
     * @throws Violations with every mistake in its declaration.
     */
    public fun <W : ItineraWorkflow> synchronous(type: Class<W>): WorkflowDescriptor<W, Sync> =
        returned(synchronous.get(type))

    /**
     * Reads the workflow [type] declares in [mode], whose steps [steps] makes factories for.
     *
     * @throws Violations with every mistake in its declaration.
     */
    public fun <W, M : Mode> read(
        type: Class<*>,
        mode: M,
        steps: StepFactories<M>,
    ): WorkflowDescriptor<W, M> = Reading(type, mode, steps).descriptor()
}

/**
 * Makes the factory of a step class that a workflow class declares, which builds the step with
 * [constructor][StepFactories.of] and runs it with its method `run`.
 *
 * Part of itinera's engine, not of its API.
 */
public fun interface StepFactories<M : Mode> {
    /** The factory of a step built with [constructor] and run with [run]. */
    public fun of(constructor: Constructor<*>, run: Method): StepFactory<M>
}

private fun synchronousStep(constructor: Constructor<*>, run: Method): StepFactory<Sync> =
    SyncStepFactory {
        val step = constructed(constructor)
        SyncStepRun { returned(invoked(run, step)) }
    }
