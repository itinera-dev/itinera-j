package org.itineradev.itinera.workflow.kotlin

import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowBuilder
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * Starts the descriptor of a synchronous workflow named [name], whose instances are created with
 * objects of [W], as `WorkflowDescriptor.builder(W::class.java, name)` does.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @sample org.itineradev.itinera.workflow.kotlin.samples.reifiedBuilding
 */
public inline fun <reified W> builder(name: String): WorkflowBuilder<W, Sync> =
    WorkflowDescriptor.builder(W::class.java, name)
