package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks an interface as a role: operations a workflow's own code implements, which the hooks of its
 * policies may call. A workflow provides the roles it implements. A hook requests one with
 * `Requests.role`, or, in a policy written as a class, with a parameter of the role's type marked
 * `@Role`, and receives an object implementing only that interface, so it reaches the workflow
 * through the role alone.
 *
 * @sample org.itineradev.itinera.policy.samples.notifying
 * @sample org.itineradev.itinera.workflow.annotated.samples.telling
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
public annotation class Role
