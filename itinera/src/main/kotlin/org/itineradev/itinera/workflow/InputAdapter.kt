package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks a method of a workflow class as an input adapter of the workflow, attached to [steps]: an
 * ordinary method in a synchronous workflow, a `suspend` one in an asynchronous workflow, returning
 * [Supplied], which the workflow interfaces' `supply` and `fromDataBag` make. Its parameters are
 * its requests: the key being resolved, marked [Key], data from the workflow, marked
 * `@FromWorkflow`, and the step's name, the journey ID and the data bag's access, by their types.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.pricing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class InputAdapter(
    /** The names of the steps it is attached to; a step has at most one adapter. */
    val steps: Array<String>,
    /** The adapter's name, unique among the workflow's adapters; the method's name when empty. */
    val name: String = "",
)
