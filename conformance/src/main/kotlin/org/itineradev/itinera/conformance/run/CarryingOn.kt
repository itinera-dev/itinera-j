package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.policy.HookReporter
import org.itineradev.itinera.policy.async.AsyncHookReporter
import org.itineradev.itinera.step.StepReporter
import org.itineradev.itinera.step.async.AsyncStepReporter

/**
 * A call a step or a hook run alone made on its reporter: the [method], its first argument [key],
 * the message of an event or the key of an entry, and whether it was [interrupted].
 */
internal data class Call(val method: String, val key: String, val interrupted: Boolean)

/**
 * The calls made on a reporter lent to a step or a hook run alone, in order. A call that is
 * interrupted is recorded so, and the step or hook carries on, as a test would let it.
 */
internal class CarryingOn {
    private val made = mutableListOf<Call>()

    /** Every call made, in order. */
    val calls: List<Call>
        get() = made.toList()

    /** Records that [method] was called with [key], and whether it was interrupted. */
    fun record(method: String, key: String, interrupted: Boolean) {
        made += Call(method, key, interrupted)
    }
}

/** Makes [call], the call of [method] with [key], recording it on [calls] and carrying on. */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
private inline fun carriedOn(calls: CarryingOn, method: String, key: String, call: () -> Unit) {
    val interrupted =
        try {
            call()
            false
        } catch (interruption: Exception) {
            true
        }
    calls.record(method, key, interrupted)
}

/** [lent], each of whose calls is recorded on [calls] and carried on past. */
internal class CarriedOnStepReporter(
    private val lent: StepReporter,
    private val calls: CarryingOn,
) : StepReporter {
    override fun info(message: String) = carriedOn(calls, "info", message) { lent.info(message) }

    override fun info(message: String, data: Any?) =
        carriedOn(calls, "info", message) { lent.info(message, data) }

    override fun warning(message: String) =
        carriedOn(calls, "warning", message) { lent.warning(message) }

    override fun warning(message: String, data: Any?) =
        carriedOn(calls, "warning", message) { lent.warning(message, data) }

    override fun error(message: String) = carriedOn(calls, "error", message) { lent.error(message) }

    override fun error(message: String, data: Any?) =
        carriedOn(calls, "error", message) { lent.error(message, data) }

    override fun setGlobalMetadata(key: String, value: Any?) =
        carriedOn(calls, SETTING, key) { lent.setGlobalMetadata(key, value) }
}

/** The asynchronous twin of [CarriedOnStepReporter]. */
internal class CarriedOnAsyncStepReporter(
    private val lent: AsyncStepReporter,
    private val calls: CarryingOn,
) : AsyncStepReporter {
    override suspend fun info(message: String) =
        carriedOn(calls, "info", message) { lent.info(message) }

    override suspend fun info(message: String, data: Any?) =
        carriedOn(calls, "info", message) { lent.info(message, data) }

    override suspend fun warning(message: String) =
        carriedOn(calls, "warning", message) { lent.warning(message) }

    override suspend fun warning(message: String, data: Any?) =
        carriedOn(calls, "warning", message) { lent.warning(message, data) }

    override suspend fun error(message: String) =
        carriedOn(calls, "error", message) { lent.error(message) }

    override suspend fun error(message: String, data: Any?) =
        carriedOn(calls, "error", message) { lent.error(message, data) }

    override suspend fun setGlobalMetadata(key: String, value: Any?) =
        carriedOn(calls, SETTING, key) { lent.setGlobalMetadata(key, value) }
}

/** [lent], each of whose calls is recorded on [calls] and carried on past. */
internal class CarriedOnHookReporter(
    private val lent: HookReporter,
    private val calls: CarryingOn,
) : HookReporter {
    override fun info(message: String) = carriedOn(calls, "info", message) { lent.info(message) }

    override fun info(message: String, data: Any?) =
        carriedOn(calls, "info", message) { lent.info(message, data) }

    override fun warning(message: String) =
        carriedOn(calls, "warning", message) { lent.warning(message) }

    override fun warning(message: String, data: Any?) =
        carriedOn(calls, "warning", message) { lent.warning(message, data) }

    override fun error(message: String) = carriedOn(calls, "error", message) { lent.error(message) }

    override fun error(message: String, data: Any?) =
        carriedOn(calls, "error", message) { lent.error(message, data) }

    override fun setGlobalMetadata(key: String, value: Any?) =
        carriedOn(calls, SETTING, key) { lent.setGlobalMetadata(key, value) }
}

/** The asynchronous twin of [CarriedOnHookReporter]. */
internal class CarriedOnAsyncHookReporter(
    private val lent: AsyncHookReporter,
    private val calls: CarryingOn,
) : AsyncHookReporter {
    override suspend fun info(message: String) =
        carriedOn(calls, "info", message) { lent.info(message) }

    override suspend fun info(message: String, data: Any?) =
        carriedOn(calls, "info", message) { lent.info(message, data) }

    override suspend fun warning(message: String) =
        carriedOn(calls, "warning", message) { lent.warning(message) }

    override suspend fun warning(message: String, data: Any?) =
        carriedOn(calls, "warning", message) { lent.warning(message, data) }

    override suspend fun error(message: String) =
        carriedOn(calls, "error", message) { lent.error(message) }

    override suspend fun error(message: String, data: Any?) =
        carriedOn(calls, "error", message) { lent.error(message, data) }

    override suspend fun setGlobalMetadata(key: String, value: Any?) =
        carriedOn(calls, SETTING, key) { lent.setGlobalMetadata(key, value) }
}

/** The method that sets an entry of global metadata. */
internal const val SETTING = "setGlobalMetadata"
