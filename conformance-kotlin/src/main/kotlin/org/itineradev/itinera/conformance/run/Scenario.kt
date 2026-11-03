package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.policy.Role

/** The role the cases call `notifier`, whose operations the scenario scripts. */
@Role
internal interface Notifier {
    /** Performs [operation] of the role. */
    fun perform(operation: String)
}

/** The interface of each role the cases name. */
private val roles: Map<String, Class<Notifier>> = mapOf("notifier" to Notifier::class.java)

/** The interface of the role the cases call [name]. */
internal fun roleOf(name: String): Class<Notifier> =
    roles[name] ?: throw ModelException("$name is not a role of the cases")

/**
 * The workflow class of every scenario, which declares nothing: the builder puts each scenario's
 * workflow together on it. It provides every role of the cases, whose operations the [witness]
 * counts, failing those among [failing], each a role and an operation.
 */
internal class Scenario(
    private val witness: Witness,
    private val failing: Set<Pair<String, String>>,
) : Notifier {
    override fun perform(operation: String) {
        witness.roleCalled("notifier", operation)
        if ("notifier" to operation in failing) {
            throw IllegalStateException("the operation $operation of the role notifier failed")
        }
    }
}
