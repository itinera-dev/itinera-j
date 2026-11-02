package org.itineradev.itinera.engine

import org.itineradev.itinera.event.FailureRequester
import org.itineradev.itinera.event.MissingData
import org.itineradev.itinera.event.RequiredDataMissing
import org.itineradev.itinera.event.WrongData
import org.itineradev.itinera.event.WrongType as WrongTypeAbort
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.StepName

/**
 * What custom code called after an attempt of [step] acts on: what the attempt [left], and the
 * [requester] that `required data missing` and `wrong type` name when it requests the failure's
 * reason or the error.
 */
internal class ActingOn(val left: Left, val step: StepName, val requester: FailureRequester)

/**
 * The reason the attempt [acting] on left reported, with its own copy of the details, or `null`
 * when an optional request finds none.
 *
 * @throws JourneyAbortSignal when a required request finds none, with `required data missing`, or
 *   when copying its details throws, with `wrong type`.
 */
internal fun Journey.reasonFor(acting: ActingOn, asked: ReasonAsked): Reason? {
    val reason =
        acting.left.reason
            ?: return absent(asked.required, MissingData.FailureReason(acting.requester), acting)
    return when (
        val receipt = Values.receive(Captured(reason.details), Values.token<Any?>(AnyValueType))
    ) {
        is Received -> reason.copy(details = receipt.value)
        is WrongType ->
            throw abort(
                Abort(
                    WrongTypeAbort(
                        acting.step,
                        WrongData.FailureReason(acting.requester),
                        receipt.error?.let(::messageOf),
                    ),
                    receipt.error,
                )
            )
    }
}

/**
 * The error that ended the attempt [acting] on, as a copy of its own, or `null` when an optional
 * request finds none.
 *
 * @throws JourneyAbortSignal when a required request finds none, with `required data missing`.
 */
internal fun Journey.errorFor(acting: ActingOn, asked: ErrorAsked): Throwable? {
    val error =
        acting.left.error
            ?: return absent(asked.required, MissingData.Error(acting.requester), acting)
    return ownCopyOf(error)
}

private fun Journey.absent(
    required: Boolean,
    missing: MissingData,
    acting: ActingOn,
): Nothing? =
    if (required) throw abort(Abort(RequiredDataMissing(acting.step, missing), null)) else null
