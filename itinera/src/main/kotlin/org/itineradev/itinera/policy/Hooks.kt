package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks the method of a step policy that is its `on step success`, called after an attempt
 * succeeded. It returns [OnSuccess].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnStepSuccess

/**
 * Marks the method of a step policy that is its `on step failure`, called once a step is given up
 * and ends as failed. It returns [OnFailure], and may request the failure's cause by its type,
 * [FailureCause][org.itineradev.itinera.journey.FailureCause].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnStepFailure

/**
 * Marks the method of a step policy that is its `on step retry`, called before a step is attempted
 * again. It returns [OnFailure], and may request the retry's cause by its type, [RetryCause].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnStepRetry

/**
 * Marks the method of a step policy that is its `on step abnormal termination`, called after an
 * attempt ended in an abnormal termination. It returns [OnFailure].
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.auditing
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnStepAbnormalTermination

/**
 * Marks the method of a workflow policy that is its `on workflow success`, called once the journey
 * succeeded, before its result. It returns nothing.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.telling
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnWorkflowSuccess

/**
 * Marks the method of a workflow policy that is its `on workflow failure`, called once the journey
 * failed, before its result. It returns nothing.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.telling
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class OnWorkflowFailure
