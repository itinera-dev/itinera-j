package org.itineradev.itinera.policy.async

import kotlin.reflect.KClass
import org.itineradev.itinera.engine.returned
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.async.asyncPartClasses

/**
 * The descriptor of an asynchronous step policy written as a class marked `@StepPolicy`, named as
 * it says, built with its constructor for every attempt, and whose hooks are its `suspend` methods
 * marked with a hook's annotation.
 *
 * @throws Violations with every mistake in the class's declaration.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncDescribingParts
 */
public fun <P : ItineraStepPolicy> asyncStepPolicyOf(
    type: KClass<P>
): StepPolicyDescriptor<P, Async> = returned(asyncPartClasses.stepPolicy(type.java))

/**
 * The descriptor of an asynchronous workflow policy written as a class marked `@WorkflowPolicy`,
 * named as it says, built with its constructor once per journey, and whose hooks are its `suspend`
 * methods marked with a hook's annotation.
 *
 * @throws Violations with every mistake in the class's declaration.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncDescribingParts
 */
public fun <P : ItineraWorkflowPolicy> asyncWorkflowPolicyOf(
    type: KClass<P>
): WorkflowPolicyDescriptor<P, Async> = returned(asyncPartClasses.workflowPolicy(type.java))
