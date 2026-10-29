package org.itineradev.itinera.workflow

import org.itineradev.itinera.Name

/**
 * The name of a workflow, which every event of its journeys carries.
 *
 * @sample org.itineradev.itinera.samples.names
 */
public class WorkflowName private constructor(name: String) : Name(name, "workflow") {
    /** Makes workflow names. */
    public companion object {
        /**
         * The workflow name [name].
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic public fun of(name: String): WorkflowName = WorkflowName(name)
    }
}
