package org.itineradev.itinera.workflow

import kotlin.reflect.KClass
import org.itineradev.itinera.policy.ItineraStepPolicy
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep

/**
 * Adds a step to a workflow written as an annotated class: a class implementing [ItineraStep],
 * built anew for every attempt. Written once per step, in the order the steps run.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class WithStep(
    /** The step's class. */
    val step: KClass<out ItineraStep>,
    /** The step's name, unique in its workflow and never empty. */
    val name: String,
    /** How many times a failed attempt may be retried after the first: never below 0. */
    val retryBudget: Int = 0,
    /** Whether an abnormal termination fails the step at once, whatever its retry budget. */
    val noRetryOnAbnormalTermination: Boolean = false,
    /** The step's policies, each a class marked `@StepPolicy`, in the order their hooks run. */
    val policies: Array<KClass<out ItineraStepPolicy>> = [],
    /**
     * The step's rescue step, a class marked `@RescueStep`, or [ItineraRescueStep] itself, the
     * default, for none. A step with a rescue step has a retry budget of 0.
     */
    val rescue: KClass<out ItineraRescueStep> = ItineraRescueStep::class,
)
