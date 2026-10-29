package org.itineradev.itinera.workflow

import org.itineradev.itinera.journey.DataBag

/**
 * Produces the journey ID of each instance of a workflow, while the instance is created, from the
 * workflow object and the instance's initial data. It is synchronous in both modes: an asynchronous
 * application that needs to wait for an ID waits first, and passes what it got in the initial data.
 *
 * Without one, each instance gets a UUID version 4. The ID is a string that is never empty: an
 * empty one, `null` from Java, or anything the generator throws makes creating the instance fail.
 *
 * @param W the class of the workflow object.
 * @sample org.itineradev.itinera.workflow.samples.ids
 */
public fun interface JourneyIdGenerator<in W> {
    /**
     * The journey ID of a new instance of the workflow object, which may read the instance's
     * initial data.
     *
     * @sample org.itineradev.itinera.workflow.samples.ids
     */
    public fun generate(workflow: W, data: DataBag): String
}
