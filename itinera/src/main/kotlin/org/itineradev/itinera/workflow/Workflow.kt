package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Declares an [ItineraWorkflow] or an `ItineraAsyncWorkflow` class as a workflow and names it;
 * every event of its journeys carries the name. Without it, the workflow is named after the class's
 * simple name, which obfuscators rename, so naming it is advised.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class Workflow(
    /** The workflow's name, which is never empty. */
    val name: String
)
