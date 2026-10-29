package org.itineradev.itinera.service

/**
 * Marks a method of a workflow class, or of a superclass, as a service provider, returning
 * [Provided] of the service's type, which `provide` and `noInstance` of the workflow's interface
 * make. Its parameters are its requests: data marked `@FromWorkflow`, and the journey ID, its
 * reporter and, of attempt scope only, the step's name and the attempt, by their types. It is
 * `suspend` exactly when the workflow is asynchronous.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.providing
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class Provider(
    /**
     * Whether it is of attempt scope, called for every attempt that needs its service, rather than
     * of journey scope, called once for the journey.
     */
    val perAttempt: Boolean = false,
    /** The steps it is attached to; none when it serves every step. */
    val steps: Array<String> = [],
    /** The key of the service it supplies, or the method's name when empty. */
    val key: String = "",
    /** Its name, or the method's name when empty. */
    val name: String = "",
)
