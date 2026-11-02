package org.itineradev.itinera.policy

import org.itineradev.itinera.Name

/**
 * The name of a policy, unique in its workflow, which every event from or about its hooks carries.
 *
 * @sample org.itineradev.itinera.samples.names
 */
public class PolicyName private constructor(name: String) : Name(name, "policy") {
    /** Makes policy names. */
    public companion object {
        /**
         * The policy name [name].
         *
         * @throws IllegalArgumentException when [name] is empty.
         * @sample org.itineradev.itinera.samples.names
         */
        @JvmStatic public fun of(name: String): PolicyName = PolicyName(name)
    }
}
