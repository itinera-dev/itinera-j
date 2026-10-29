package org.itineradev.itinera.workflow

/**
 * Marks the method of a workflow class that produces each instance's journey ID, as a
 * [JourneyIdGenerator] does. It returns a `String`, is never `suspend`, and takes either nothing or
 * the instance's initial data as a `DataBag`. A class has at most one.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class IdGenerator
