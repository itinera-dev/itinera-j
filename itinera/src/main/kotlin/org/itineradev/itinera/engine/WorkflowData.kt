package org.itineradev.itinera.engine

import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.OptionalInputAbsent
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.WrongType as WrongTypeAbort
import org.itineradev.itinera.journey.Abort

/**
 * Reads data from the workflow under [key] for [by], as [type]: its own copy of the value in the
 * data bag, or `null` when an [optional][required] request finds none, which emits
 * `optional_input_absent`.
 *
 * @throws JourneyAbortSignal when a required request finds no value, when the value is of another
 *   type, or when copying it throws, and once the journey is aborted.
 */
@JvmSynthetic
internal suspend fun Journey.fromWorkflow(
    key: String,
    type: ValueType,
    required: Boolean,
    by: RequestedBy,
): Any? {
    val stored = data.stored(key) ?: return absent(key, required, by)
    return typed(stored, key, type, by)
}

/**
 * The receiver's own copy of [stored], read as [type] for [by] under [key].
 *
 * @throws JourneyAbortSignal when the value is of another type, or when copying it throws.
 */
internal fun Journey.typed(stored: Captured, key: String, type: ValueType, by: RequestedBy): Any? =
    when (val receipt = Values.receive(stored, Values.token<Any?>(type))) {
        is Received -> receipt.value
        is WrongType -> throw wrongType(key, by, receipt.error)
    }

private suspend fun Journey.absent(key: String, required: Boolean, by: RequestedBy): Any? {
    if (required) {
        throw abort(Abort(RequiredDataMissing(by.step, MissingData.Key(key, by.requester)), null))
    }
    emit { OptionalInputAbsent(sequence, timestamp, journeyId, workflow, key, by.source) }
    return null
}

private fun Journey.wrongType(key: String, by: RequestedBy, error: Throwable?): JourneyAbortSignal =
    abort(Abort(WrongTypeAbort(by.step, key, by.requester, error?.let(::messageOf)), error))
