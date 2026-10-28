package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks a parameter of a step's constructor as one of its inputs: data from the workflow, read
 * under its key as the parameter's type.
 *
 * The input is optional when the parameter's type admits `null`, as `Long?` in Kotlin, or, in a
 * class written in Java with `itinera-java`, when it is an `Optional`; otherwise it is required. A
 * required input whose value may itself be `null` sets [required].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.charging
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
public annotation class Input(
    /**
     * The key, or the parameter's name when empty. Java keeps parameters' names only when compiled
     * with `-parameters`.
     */
    val value: String = "",
    /** Whether the input is required though its type admits `null`. */
    val required: Boolean = false,
)
