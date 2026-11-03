package org.itineradev.itinera.workflow.checker

import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.OnStepAbnormalTermination
import org.itineradev.itinera.policy.OnStepFailure
import org.itineradev.itinera.policy.OnStepRetry
import org.itineradev.itinera.policy.OnStepSuccess
import org.itineradev.itinera.policy.OnWorkflowFailure
import org.itineradev.itinera.policy.OnWorkflowSuccess
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.policy.async.asyncStepPolicyOf
import org.itineradev.itinera.policy.async.asyncWorkflowPolicyOf
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.RescueStep
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.async.asyncRescueStepOf
import org.itineradev.itinera.workflow.async.asyncStep

/** The parts [fixture] declares, as its nested classes marked as a step, rescue step or policy. */
internal fun partClassesOf(fixture: Class<*>): List<Class<*>> =
    fixture.declaredClasses.filter(::isPart)

/**
 * The violations reading the part [type] alone finds, in the mode its own methods say: asynchronous
 * when its methods named `run`, or its hooks, are all `suspend`, and there is one at least.
 */
internal fun loneViolationsOf(type: Class<*>): List<Violation> =
    try {
        readAlone(type, suspends(type))
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }

@Suppress("UNCHECKED_CAST")
private fun readAlone(type: Class<*>, suspends: Boolean) {
    when {
        type.isAnnotationPresent(Step::class.java) ->
            if (suspends) asyncStep(type.simpleName, type.kotlin as KClass<ItineraStep>)
            else StepDescriptor.builder(type.simpleName, type as Class<ItineraStep>)
        type.isAnnotationPresent(RescueStep::class.java) ->
            if (suspends) asyncRescueStepOf(type.kotlin as KClass<ItineraRescueStep>)
            else RescueStepDescriptor.of(type as Class<ItineraRescueStep>)
        type.isAnnotationPresent(StepPolicy::class.java) ->
            if (suspends) asyncStepPolicyOf(type.kotlin as KClass<ItineraStepPolicy>)
            else StepPolicyDescriptor.of(type as Class<ItineraStepPolicy>)
        else ->
            if (suspends) asyncWorkflowPolicyOf(type.kotlin as KClass<ItineraWorkflowPolicy>)
            else WorkflowPolicyDescriptor.of(type as Class<ItineraWorkflowPolicy>)
    }
}

private fun isPart(type: Class<*>): Boolean = parts.any(type::isAnnotationPresent)

private val parts =
    listOf(
        Step::class.java,
        RescueStep::class.java,
        StepPolicy::class.java,
        WorkflowPolicy::class.java,
    )

private fun suspends(type: Class<*>): Boolean {
    val methods = type.methods.filter(::isPartMethod)
    return methods.isNotEmpty() && methods.all(::isSuspend)
}

private fun isPartMethod(method: Method): Boolean =
    method.name == "run" || hooks.any(method::isAnnotationPresent)

/** Whether [method] is a `suspend` function, which takes a continuation last. */
private fun isSuspend(method: Method): Boolean =
    method.parameterTypes.lastOrNull() == Continuation::class.java

private val hooks =
    listOf(
        OnStepSuccess::class.java,
        OnStepFailure::class.java,
        OnStepRetry::class.java,
        OnStepAbnormalTermination::class.java,
        OnWorkflowSuccess::class.java,
        OnWorkflowFailure::class.java,
    )
