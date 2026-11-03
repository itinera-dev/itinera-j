package org.itineradev.itinera.step

/**
 * Marks a class implementing [ItineraRescueStep] as a rescue step, which a workflow gives a step
 * with `@WithStep(rescue = ...)`. Its one constructor takes the rescue step's inputs, each marked
 * [Input], the data it requests from the step it rescues, each marked [FromRescuedStep], and the
 * failure's [Reason] and its error, a `Throwable`, by their types, each optional when its type
 * admits `null` or, in Java with `itinera-java`, is an `Optional`. Its one public method `run`
 * takes its services, each marked `@Service`, and the handles it requests by their types: a
 * [org.itineradev.itinera.journey.Contributor] and the [StepReporter] of its mode.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.renewing
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class RescueStep
