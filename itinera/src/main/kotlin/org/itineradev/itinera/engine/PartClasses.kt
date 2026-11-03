package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepBuilder
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Violations

/**
 * Reads the class of one annotated part alone, outside any workflow class, in [mode]: a step, a
 * rescue step or a policy, whose steps [steps] makes factories for and whose hooks [hooks] calls.
 * Each read throws [Violations] with every mistake in the class.
 *
 * Part of itinera's engine, not of its API.
 */
public class PartClasses<M : Mode>(
    private val mode: M,
    private val steps: StepFactories<M>,
    private val hooks: HookCalls<M>,
) {
    /**
     * The builder of the step named [name] that [type] declares, with what it requests.
     *
     * @throws Violations with every mistake in the class.
     */
    public fun step(name: StepName, type: Class<*>): StepBuilder<M> = read { problems ->
        StepClasses(mode, steps, problems).read(name, type)?.let { step ->
            steps.builder(name, step)
        }
    }

    /**
     * The descriptor of the rescue step [type] declares.
     *
     * @throws Violations with every mistake in the class.
     */
    public fun rescue(type: Class<*>): RescueStepDescriptor<M> = read { problems ->
        RescueClasses(mode, steps, problems)
            .read("rescue step ${type.simpleName}", type)
            ?.let(steps::rescueStep)
    }

    /**
     * The descriptor of the step policy [type] declares.
     *
     * @throws Violations with every mistake in the class.
     */
    public fun stepPolicy(type: Class<*>): StepPolicyDescriptor<Any, M> = read { problems ->
        PolicyClasses(mode, hooks, problems).stepPolicy(type)
    }

    /**
     * The descriptor of the workflow policy [type] declares.
     *
     * @throws Violations with every mistake in the class.
     */
    public fun workflowPolicy(type: Class<*>): WorkflowPolicyDescriptor<Any, M> = read { problems ->
        PolicyClasses(mode, hooks, problems).workflowPolicy(type)
    }

    private fun <T : Any> read(reading: (Problems) -> T?): T {
        val problems = Problems()
        val read = reading(problems)
        if (problems.found || read == null) throw Violations(problems.violations)
        return read
    }
}

/** The builder of the step [step] declares, named [name], with what it requests. */
internal fun <M : Mode> StepFactories<M>.builder(
    name: StepName,
    step: StepClass<M>,
): StepBuilder<M> {
    val builder = StepBuilder(name, of(step))
    step.requests.forEach(builder::request)
    return builder
}

/** The descriptor of the rescue step [rescue] declares, with what it requests. */
internal fun <M : Mode> StepFactories<M>.rescueStep(
    rescue: RescueClass<M>
): RescueStepDescriptor<M> {
    val builder = RescueStepBuilder(of(rescue))
    rescue.requests.forEach(builder::request)
    return builder.build()
}
