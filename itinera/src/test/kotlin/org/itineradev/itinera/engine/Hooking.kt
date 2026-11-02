package org.itineradev.itinera.engine

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.StepPolicyBuilder
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicyBuilder
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor

/** Policies for the tests of how the engine builds policies and calls their hooks. */
@OptIn(ExperimentalItineraApi::class)
object Hooking {
    /** A policy that keeps nothing: its hooks receive all they use. */
    class Audit

    /** The step policy `audit`, to which each test adds the hooks it calls. */
    fun audit(): StepPolicyBuilder<Audit> =
        StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))

    /** The workflow policy `ledger`, to which each test adds the hooks it calls. */
    fun ledger(): WorkflowPolicyBuilder<Audit> =
        WorkflowPolicyDescriptor.builder("ledger", PolicyFactory(::Audit))
}
