package org.itineradev.itinera.workflow.checker

/**
 * The mistakes in how [workflow] is put together, which the run-time reader finds once the class
 * declares no mistake: the violations of how a workflow is put together that the specification
 * names, for its steps, services, providers, policies and input adapters.
 */
internal fun assemblyMistakes(workflow: ReadWorkflow): List<Finding> {
    val steps = stepNamesOf(workflow)
    val origin = workflow.origin
    return duplicateStepNames(steps, origin) +
        workflow.steps.filter(::isRescuedAndRetried).map { step ->
            rescuedStepRetried(step, origin)
        } +
        serviceMistakes(workflow) +
        providerMistakes(workflow, steps.toSet()) +
        policyMistakes(workflow) +
        adapterMistakes(workflow, steps.toSet())
}

/** A `duplicate step name` for each name shared by several of [names]. */
internal fun duplicateStepNames(names: List<String>, origin: Any): List<Finding> =
    shared(names).map { name -> duplicateStepName(name, origin) }

private fun duplicateStepName(name: String, origin: Any): Finding =
    Named("duplicate step name", "two steps are named $name", origin)

/** Each of [values] that occurs more than once among them, once, in the order first met. */
internal fun <T> shared(values: List<T>): List<T> =
    values.groupingBy(::itself).eachCount().filterValues(::isShared).keys.toList()

private fun <T> itself(value: T): T = value

private fun isShared(count: Int): Boolean = count > 1

/** Whether [values] holds more than one value. */
internal fun isSharedList(values: List<*>): Boolean = values.size > 1

private fun stepNamesOf(workflow: ReadWorkflow): List<String> = workflow.steps.flatMap(::namesOf)

private fun namesOf(step: DeclaredStep): List<String> =
    if (step.rescue == null) listOf(step.name) else listOf(step.name, "${step.name}_rescue")

private fun isRescuedAndRetried(step: DeclaredStep): Boolean =
    step.rescue != null && step.retryBudget > 0

private fun rescuedStepRetried(step: DeclaredStep, origin: Any): Finding =
    Named(
        "rescued step retried",
        "step ${step.name} has a rescue step and a retry budget above 0",
        origin,
    )
