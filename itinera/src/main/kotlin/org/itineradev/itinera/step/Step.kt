package org.itineradev.itinera.step

/**
 * Marks a class implementing [ItineraStep] as a step, which a workflow adds with `@WithStep`. Its
 * one constructor takes the step's inputs, each marked [Input], and its one public method `run`
 * takes its services, each marked `@Service`, and the handles it requests by their types: a
 * [org.itineradev.itinera.journey.Contributor] and the [StepReporter] of its mode. The executor
 * builds it anew for every attempt.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class Step
