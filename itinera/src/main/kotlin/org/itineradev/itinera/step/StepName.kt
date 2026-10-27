package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.Name

/**
 * The name of a step, unique in its workflow, which every event about the step carries.
 *
 * @sample org.itineradev.itinera.samples.names
 */
@ExperimentalItineraApi
public class StepName private constructor(name: String) : Name(name, "step") {
    /** Makes step names. */
    public companion object {
        /**
         * The step name [name].
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic public fun of(name: String): StepName = StepName(name)
    }
}
