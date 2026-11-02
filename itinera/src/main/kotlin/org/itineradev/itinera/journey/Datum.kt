package org.itineradev.itinera.journey

/**
 * Marks a component of a record or data class that a journey's initial data is given as, or its
 * output read as, to name its key or make it required. A component that is not marked is keyed by
 * its name.
 *
 * A component is optional when its type admits `null`, as `String?` in a Kotlin class read with
 * `itinera-kotlin`, or, in a record written in Java with `itinera-java`, when it is an `Optional`;
 * otherwise it is required. An optional component gives no key when it holds nothing, and takes
 * `null`, or an empty `Optional`, when its key holds nothing. A required component whose value may
 * itself be `null` sets [required]: it always gives its key, and its key must hold a value, `null`
 * included. An `Optional` component is never marked required.
 *
 * @sample org.itineradev.itinera.journey.kotlin.samples.outputs
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
@MustBeDocumented
public annotation class Datum(
    /** The key, or the component's name when empty. */
    val key: String = "",
    /** Whether the component is required though its type admits `null`. */
    val required: Boolean = false,
)
