package org.itineradev.itinera.engine

import kotlin.reflect.KClass
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
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
    hooks: HookCalls<M>,
) {
    private val problems = Problems()
    private val steps = StepClasses(mode, factories, problems)
    private val policies = PolicyClasses(mode, hooks, problems)

    /** @throws Violations with every mistake found. */
    fun <W> descriptor(): WorkflowDescriptor<W, M> {
        problems.readable(type)
        val workflow = type.getAnnotation(Workflow::class.java)
        val name = named(workflow)
        val declared = type.getAnnotationsByType(WithStep::class.java).map(::step)
        val attached = workflow?.policies.orEmpty().map(::workflowPolicy)
        val generator = IdGenerators(problems).of<W>(type)
        problems += duplicateStepNames(declared.mapNotNull(Declared<M>::name))
        if (problems.found || name == null) throw Violations(problems.violations)
        return Checked.of(
            name,
            mode,
            declared.mapNotNull(Declared<M>::descriptor),
            attached.filterNotNull(),
            rolesOf<W>(type),
            emptyList(),
            generator,
        )
    }

    private fun workflowPolicy(policy: KClass<*>): WorkflowPolicyDescriptor<Any, M>? =
        policies.workflowPolicy(policy.java)

    private fun stepPolicy(policy: KClass<*>): StepPolicyDescriptor<Any, M>? =
        policies.stepPolicy(policy.java)

    private fun named(workflow: Workflow?): WorkflowName? {
        val name = workflow?.name ?: type.simpleName
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
        val attached = all(declared.policies.map(::stepPolicy))
        val descriptor =
            if (name == null || step == null || !budgeted) null
            else attached?.let { policies -> described(name, step, policies, declared) }
        return Declared(name, descriptor)
    }

    private fun described(
        name: StepName,
        step: StepClass<M>,
        attached: List<StepPolicyDescriptor<Any, M>>,
        declared: WithStep,
    ): StepDescriptor<M> {
        val builder = StepBuilder(name, factories.of(step)).retryBudget(declared.retryBudget)
        step.requests.forEach(builder::request)
        attached.forEach(builder::policy)
        if (declared.noRetryOnAbnormalTermination) builder.noRetryOnAbnormalTermination()
        return builder.build()
    }
}

/** A step a workflow class declares: its name and descriptor, when each could be read. */
private class Declared<M : Mode>(val name: StepName?, val descriptor: StepDescriptor<M>?)
