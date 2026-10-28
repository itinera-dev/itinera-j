package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks a class implementing [ItineraWorkflowPolicy] as a workflow policy and names it. Every event
 * from or about its hooks carries the name.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.telling
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class WorkflowPolicy(
    /**
     * The policy's name, or, when empty, the class's simple name, which obfuscators rename, so
     * naming it is advised.
     */
    val name: String = ""
)
