package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks a parameter of a rescue step's constructor as data from the step it rescues: what the
 * failed attempt contributed under the key, which was not committed, read as the parameter's type.
 *
 * The request is optional when the parameter's type admits `null`, as `Long?` in Kotlin, or, in a
 * class written in Java with `itinera-java`, when it is an `Optional`; otherwise it is required. A
 * required request whose value may itself be `null` sets [required].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
public annotation class FromRescuedStep(
    /**
     * The key, or the parameter's name when empty. Java keeps parameters' names only when compiled
     * with `-parameters`.
     */
    val value: String = "",
    /** Whether the request is required though its type admits `null`. */
    val required: Boolean = false,
)
