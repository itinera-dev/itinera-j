package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A workflow policy written as a class, marked [WorkflowPolicy]: hooks that act on the journey of a
 * workflow it is attached to with `@Workflow(policies = ...)`. Each hook is a public method marked
 * [OnWorkflowSuccess] or [OnWorkflowFailure], which returns nothing: an ordinary method in a
 * synchronous workflow, and a `suspend` one in an asynchronous workflow. The executor builds the
 * class once per journey, with its constructor that takes nothing.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.telling
 */
@ExperimentalItineraApi public interface ItineraWorkflowPolicy
