package org.itineradev.itinera.service

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks a parameter of a step's method `run` as a service the step requests, under its key, of the
 * parameter's type. A service is always required, so the type never admits `null`.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.paying
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
public annotation class Service(
    /**
     * The key, or the parameter's name when empty. Java keeps parameters' names only when compiled
     * with `-parameters`.
     */
    val value: String = ""
)
