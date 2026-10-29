package org.itineradev.itinera.engine

import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * The name of the rescue step of [step], `<step name>_rescue`.
 *
 * Part of itinera's engine, not of its API.
 */
public fun rescueOf(step: StepName): StepName = StepName.of("${step}_rescue")

/**
 * The name of each step of [descriptor], each followed by its rescue step's, if it has one.
 *
 * Part of itinera's engine, not of its API.
 */
public fun stepNamesOf(descriptor: WorkflowDescriptor<*, *>): List<StepName> =
    descriptor.steps.flatMap(::namesOf)

private fun namesOf(step: StepDescriptor<*>): List<StepName> =
    if (step.rescue == null) listOf(step.name) else listOf(step.name, rescueOf(step.name))

/** A `rescued step retried` for each of [steps] with both a rescue step and retries. */
internal fun rescuedStepsRetried(steps: List<StepDescriptor<*>>): List<Violation> =
    steps.filter(::isRescuedAndRetried).map(::rescuedStepRetried)

private fun isRescuedAndRetried(step: StepDescriptor<*>): Boolean =
    step.rescue != null && step.retryBudget > 0

private fun rescuedStepRetried(step: StepDescriptor<*>): Violation =
    Violation.RescuedStepRetried(step.name)
