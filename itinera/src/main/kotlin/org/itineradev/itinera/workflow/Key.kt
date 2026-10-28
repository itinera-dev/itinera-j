package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks the `String` parameter of an input adapter's method that receives the key of the input
 * being resolved.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.pricing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
public annotation class Key
