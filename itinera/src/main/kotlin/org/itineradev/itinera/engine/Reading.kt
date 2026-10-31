package org.itineradev.itinera.engine

import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow
import org.itineradev.itinera.workflow.WorkflowDescriptor
import org.itineradev.itinera.workflow.WorkflowName

/** Reads the workflow an annotated class declares in [mode], finding every mistake at once. */
internal class Reading<M : Mode>(
    private val type: Class<*>,
    private val mode: M,
    private val factories: StepFactories<M>,
) {
    private val problems = Problems()
    private val steps = StepClasses(mode, factories, problems)

    /** @throws Violations with every mistake found. */
    fun <W> descriptor(): WorkflowDescriptor<W, M> {
        problems.readable(type)
        val name = named()
        val declared = type.getAnnotationsByType(WithStep::class.java).map(::step)
        val generator = IdGenerators(problems).of<W>(type)
        problems += duplicateStepNames(declared.mapNotNull(Declared<M>::name))
        if (problems.found || name == null) throw Violations(problems.violations)
        return Checked.of(
            name,
            mode,
            declared.mapNotNull(Declared<M>::descriptor),
            emptyList(),
            emptyList(),
            generator,
        )
    }

    private fun named(): WorkflowName? {
        val name = type.getAnnotation(Workflow::class.java)?.name ?: type.simpleName
        if (name.isEmpty()) problems.misdeclared(type.name, "names its workflow with an empty name")
        return name.takeUnless(String::isEmpty)?.let(WorkflowName::of)
    }

    private fun step(declared: WithStep): Declared<M> {
        val name = declared.name.takeUnless(String::isEmpty)?.let(StepName::of)
        if (name == null) problems.misdeclared(type.name, "names a step with an empty name")
        val budgeted = declared.retryBudget >= 0
        if (!budgeted)
            problems.misdeclared(type.name, "gives step ${declared.name} a retry budget below 0")
        val step = name?.let { stepName -> steps.read(stepName, declared.step.java) }
        val descriptor =
            if (name == null || step == null || !budgeted) null else described(name, step, declared)
        return Declared(name, descriptor)
    }

    private fun described(
        name: StepName,
        step: StepClass<M>,
        declared: WithStep,
    ): StepDescriptor<M> {
        val builder = StepBuilder(name, factories.of(step)).retryBudget(declared.retryBudget)
        step.requests.forEach(builder::request)
        if (declared.noRetryOnAbnormalTermination) builder.noRetryOnAbnormalTermination()
        return builder.build()
    }
}

/** A step a workflow class declares: its name and descriptor, when each could be read. */
private class Declared<M : Mode>(val name: StepName?, val descriptor: StepDescriptor<M>?)
