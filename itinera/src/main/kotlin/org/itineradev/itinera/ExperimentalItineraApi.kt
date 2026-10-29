package org.itineradev.itinera

/**
 * Marks API whose proposal itinera does not claim yet: it may change, or go, in any release.
 *
 * Kotlin refuses to compile a use of it unless the caller opts in, with
 * `@OptIn(ExperimentalItineraApi::class)` or the compiler option
 * `-opt-in=org.itineradev.itinera.ExperimentalItineraApi`. Java does not check the marker, which
 * shows only in the documentation.
 *
 * @sample org.itineradev.itinera.step.testing.samples.optingIn
 */
@RequiresOptIn(
    message =
        "This API belongs to a proposal itinera does not claim yet, and may change in any release.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
@MustBeDocumented
public annotation class ExperimentalItineraApi
