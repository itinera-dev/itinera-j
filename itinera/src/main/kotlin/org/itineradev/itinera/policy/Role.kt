package org.itineradev.itinera.policy

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Marks an interface as a role: operations a workflow's own code implements, which the hooks of its
 * policies may call. A workflow provides the roles it implements. A hook requests one with
 * `Requests.role`, and receives an object implementing only that interface, so it reaches the
 * workflow through the role alone.
 *
 * @sample org.itineradev.itinera.policy.samples.notifying
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class Role
