package org.itineradev.itinera.policy

/**
 * Marks a class implementing [ItineraStepPolicy] as a step policy and names it. Every event from or
 * about its hooks carries the name.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class StepPolicy(
    /**
     * The policy's name, or, when empty, the class's simple name, which obfuscators rename, so
     * naming it is advised.
     */
    val name: String = ""
)
